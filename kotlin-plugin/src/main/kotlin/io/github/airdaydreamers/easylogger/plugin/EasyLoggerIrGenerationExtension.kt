package io.github.airdaydreamers.easylogger.plugin

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrBlockBody
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.expressions.impl.IrGetObjectValueImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class EasyLoggerIrGenerationExtension(
    private val logAnnotations: List<String>,
    private val messageCollector: MessageCollector?, // Made nullable for now, can be configured via CLI processor
    private val logType: LogType
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        messageCollector?.report(CompilerMessageSeverity.INFO, "EasyLoggerIrGenerationExtension: Processing module ${moduleFragment.name}")
        messageCollector?.report(CompilerMessageSeverity.INFO, "EasyLoggerIrGenerationExtension: Annotations to look for: $logAnnotations")
        messageCollector?.report(CompilerMessageSeverity.INFO, "EasyLoggerIrGenerationExtension: LogType: $logType")

        if (logAnnotations.isEmpty()) {
            messageCollector?.report(CompilerMessageSeverity.WARNING, "EasyLoggerIrGenerationExtension: No debug log annotations provided.")
            return
        }

        val annotationFqNames = logAnnotations.map { FqName(it) }

        moduleFragment.transform(EasyLoggerIrTransformer(pluginContext, annotationFqNames, messageCollector, logType), null)
    }
}

private class EasyLoggerIrTransformer(
    private val pluginContext: IrPluginContext,
    private val logAnnotationFqNames: List<FqName>,
    private val messageCollector: MessageCollector?,
    private val logType: LogType
) : IrElementTransformerVoidWithContext() {

    private fun getLogTag(function: IrFunction): String {
        val tag = when (val parent = function.parent) {
            is IrClass -> parent.name.asString()
            is IrFile -> {
                val fileName = parent.name.substringAfterLast('/')
                fileName.substringBeforeLast('.', fileName.takeIf { it.contains('.') } ?: fileName)
                    .takeIf { it.isNotBlank() } ?: "UnknownFile"
            }
            else -> "DefaultTag"
        }
        // Ensure tag is not empty and respects Android's typical 23 char limit
        val nonEmptyTag = tag.takeIf { it.isNotBlank() } ?: "DefaultTag"
        return if (nonEmptyTag.length > 23) nonEmptyTag.substring(0, 23) else nonEmptyTag
    }

    @OptIn(FirIncompatiblePluginAPI::class)
    override fun visitFunctionNew(declaration: IrFunction): IrStatement {
        // Process only functions with a body and annotations
        val body = declaration.body ?: return super.visitFunctionNew(declaration)
        if (logAnnotationFqNames.none { fqName -> declaration.hasAnnotation(fqName) }) {
            return super.visitFunctionNew(declaration)
        }

        messageCollector?.report(CompilerMessageSeverity.INFO, "Transforming function: ${declaration.kotlinFqName}")

        // Get System.currentTimeMillis()
        val systemClass = pluginContext.referenceClass(FqName("java.lang.System"))
            ?: error("Cannot find java.lang.System")
        val currentTimeMillisFun = systemClass.functions.single { it.owner.name == Name.identifier("currentTimeMillis") && it.owner.valueParameters.isEmpty() }

        // Get System.out.println(String) or Log.d(String, String) based on logType
        val logFun: IrSimpleFunctionSymbol
        if (logType == LogType.PRINTLN) {
            logFun = pluginContext.referenceFunctions(FqName("kotlin.io.println"))
                .single {
                    val params = it.owner.valueParameters
                    params.size == 1 && params[0].type == pluginContext.irBuiltIns.anyNType // println takes Any?
                }
        } else { // LogType.LOG_D
            val logClass = pluginContext.referenceClass(FqName("android.util.Log"))
                ?: error("Cannot find android.util.Log. Make sure Android SDK is available.")
            logFun = logClass.functions.single {
                it.owner.name == Name.identifier("d") &&
                        it.owner.valueParameters.size == 2 &&
                        it.owner.valueParameters[0].type == pluginContext.irBuiltIns.stringType && // tag
                        it.owner.valueParameters[1].type == pluginContext.irBuiltIns.stringType    // msg
            }
        }


        // Create a builder for this function
        val irBuilder = DeclarationIrBuilder(pluginContext, declaration.symbol, declaration.startOffset, declaration.endOffset)

        // Original statements
        val originalStatements = (body as IrBlockBody).statements

        // Create new body
        declaration.body = irBuilder.irBlockBody {
            // 1. Store start time
            val startTimeVar = irTemporary(irCall(currentTimeMillisFun), nameHint = "startTime")

            // 2. Log function entry
            +irBuilder.logFunctionEntry(declaration, logFun, logType)

            // 3. Add original statements, instrumenting return statements
            for (statement in originalStatements) {
                if (statement is IrReturn) {
                    // Log before returning
                    +irBuilder.logFunctionExit(declaration, startTimeVar, logFun, logType, statement.value)
                    +statement // Add the original return
                } else {
                    +statement
                }
            }

            // 4. Handle implicit returns for Unit functions (if last statement is not IrReturn)
            if (declaration.returnType.isUnit() && (originalStatements.isEmpty() || originalStatements.lastOrNull() !is IrReturn)) {
                // Explicitly construct IrGetObjectValue for Unit
                val unitValue = IrGetObjectValueImpl(
                    this.startOffset, this.endOffset, // Use offsets from the current builder scope
                    pluginContext.irBuiltIns.unitType,
                    pluginContext.irBuiltIns.unitClass // This is IrClassSymbol for Unit
                )
                +irBuilder.logFunctionExit(declaration, startTimeVar, logFun, logType, unitValue)
            }
        }
        return super.visitFunctionNew(declaration)
    }

    private fun IrBuilderWithScope.logFunctionEntry(
        function: IrFunction,
        logFun: IrSimpleFunctionSymbol,
        logType: LogType
    ): IrExpression {
        val functionName = function.name.asString()
        var entryMessage = "⇢ $functionName("

        function.valueParameters.forEachIndexed { index, param ->
            entryMessage += "${param.name.asString()}="
            // For simplicity, we'll just append the parameter name.
            // To get actual values, we'd need irGet(param) and then convert to string.
            // This part can be complex due to types and toString() calls.
            // entryMessage += irGet(param).type.toString() // Placeholder for actual value
            entryMessage += "\${${param.name.asString()}}" // Using string template style for IR
            if (index < function.valueParameters.lastIndex) {
                entryMessage += ", "
            }
        }
        entryMessage += ")"

        val fullEntryMessage = irConcat()
        fullEntryMessage.arguments.add(irString("⇢ ${function.name}("))
        function.valueParameters.forEachIndexed { i, param ->
            fullEntryMessage.arguments.add(irString("${param.name.asString()}="))
            fullEntryMessage.arguments.add(irCall(pluginContext.irBuiltIns.extensionToString).apply {
                extensionReceiver = irGet(param)
            })
            if (i < function.valueParameters.size - 1) {
                fullEntryMessage.arguments.add(irString(", "))
            }
        }
        fullEntryMessage.arguments.add(irString(")"))

        return if (logType == LogType.PRINTLN) {
            irCall(logFun).apply {
                putValueArgument(0, fullEntryMessage)
            }
        } else { // LogType.LOG_D
            irCall(logFun).apply {
                putValueArgument(0, irString(getLogTag(function))) // TAG
                putValueArgument(1, fullEntryMessage) // MSG
            }
        }
    }

    @OptIn(FirIncompatiblePluginAPI::class)
    private fun IrBuilderWithScope.logFunctionExit(
        function: IrFunction,
        startTimeVar: IrVariable,
        logFun: IrSimpleFunctionSymbol,
        logType: LogType,
        returnValue: IrExpression? // Null for Unit functions if not explicitly returned
    ): IrExpression {
        val functionName = function.name.asString()

        // Get System.currentTimeMillis()
        val systemClass = pluginContext.referenceClass(FqName("java.lang.System"))
            ?: error("Cannot find java.lang.System")
        val currentTimeMillisFun = systemClass.functions.single { it.owner.name == Name.identifier("currentTimeMillis") && it.owner.valueParameters.isEmpty() }

        // Find Long.minus(Long) specifically
        val longMinus = pluginContext.irBuiltIns.longClass.functions.single {
            it.owner.name == Name.identifier("minus") &&
                    it.owner.valueParameters.size == 1 &&
                    it.owner.valueParameters[0].type == pluginContext.irBuiltIns.longType
        }

        val elapsedTime = irCall(longMinus).apply {
            dispatchReceiver = irCall(currentTimeMillisFun) // current time
            putValueArgument(0, irGet(startTimeVar))    // start time
        }

        val fullExitMessage = irConcat()
        fullExitMessage.arguments.add(irString("⇠ $functionName [ran in "))
        fullExitMessage.arguments.add(irCall(pluginContext.irBuiltIns.extensionToString).apply {
            extensionReceiver = elapsedTime
        })
        fullExitMessage.arguments.add(irString(" ms]"))

        if (returnValue != null && !function.returnType.isUnit()) {
            fullExitMessage.arguments.add(irString(" = "))
            fullExitMessage.arguments.add(irCall(pluginContext.irBuiltIns.extensionToString).apply {
                extensionReceiver = returnValue
            })
        }

        return if (logType == LogType.PRINTLN) {
            irCall(logFun).apply {
                putValueArgument(0, fullExitMessage)
            }
        } else { // LogType.LOG_D
            irCall(logFun).apply {
                putValueArgument(0, irString(getLogTag(function))) // TAG
                putValueArgument(1, fullExitMessage) // MSG
            }
        }
    }
}