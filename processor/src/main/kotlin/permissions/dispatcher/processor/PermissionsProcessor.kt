package permissions.dispatcher.processor

import permissions.dispatcher.RuntimePermissions
import permissions.dispatcher.processor.impl.java.JavaActivityProcessorUnit
import permissions.dispatcher.processor.impl.java.JavaFragmentProcessorUnit
import permissions.dispatcher.processor.impl.kotlin.KotlinActivityProcessorUnit
import permissions.dispatcher.processor.impl.kotlin.KotlinFragmentProcessorUnit
import permissions.dispatcher.processor.util.findAndValidateProcessorUnit
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.properties.Delegates

/** Element Utilities, obtained from the processing environment */
var ELEMENT_UTILS: Elements by Delegates.notNull()
/** Type Utilities, obtained from the processing environment */
var TYPE_UTILS: Types by Delegates.notNull()

class PermissionsProcessor : AbstractProcessor() {
    private val javaProcessorUnits = listOf(JavaActivityProcessorUnit(), JavaFragmentProcessorUnit())
    private val kotlinProcessorUnits = listOf(KotlinActivityProcessorUnit(), KotlinFragmentProcessorUnit())
    /* Processing Environment helpers */
    private var filer: Filer by Delegates.notNull()

    override fun init(processingEnv: ProcessingEnvironment) {
        super.init(processingEnv)
        filer = processingEnv.filer
        ELEMENT_UTILS = processingEnv.elementUtils
        TYPE_UTILS = processingEnv.typeUtils
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, ">>>>>>>>>>>> init: $filer")
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, ">>>>>>>>>>>> init: $ELEMENT_UTILS")
        processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, ">>>>>>>>>>>> init: $TYPE_UTILS")

    }

    override fun getSupportedSourceVersion(): SourceVersion? {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): Set<String> {
        return hashSetOf(RuntimePermissions::class.java.canonicalName)
    }

    /**
     * @param annotations:该注解处理器能处理的所有注解类型
     * @param roundEnv:囊括当前轮生成的抽象语法树RoundEnvironment
     */
    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        for (annotation in annotations) {
            processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, ">>>>>>>>>>>> annotations:  ${annotation.qualifiedName} ${annotation.kind.name}   ${annotations.size}")
        }
        // Create a RequestCodeProvider which guarantees unique request codes for each permission request
        val requestCodeProvider = RequestCodeProvider()

        // The Set of annotated elements needs to be ordered
        // in order to achieve Deterministic, Reproducible Builds
        roundEnv.getElementsAnnotatedWith(RuntimePermissions::class.java)//获取所有被@RuntimePermissions注解的类
                .sortedBy { it.simpleName.toString() }
                .forEach {
                    processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, ">>>>>>>>>>>>$it :  ${it.simpleName} ${it.kind} ")//permissions.dispatcher.sample.mainActivity
                    val rpe = RuntimePermissionsElement(it as TypeElement)
                    //@Metadata注解是kotlin专有的，因此用这个来判断当前Element是Java类还是Kotlin类
                    val kotlinMetadata = it.getAnnotation(Metadata::class.java)
                    processingEnv.messager.printMessage(Diagnostic.Kind.WARNING, ">>>>>>>>>>>>$it :  $kotlinMetadata ")//permissions.dispatcher.sample.mainActivity
                    if (kotlinMetadata != null) {
                        processKotlin(it, rpe, requestCodeProvider)
                    } else {
                        processJava(it, rpe, requestCodeProvider)
                    }
                }
        return true
    }

    private fun processKotlin(element: Element, rpe: RuntimePermissionsElement, requestCodeProvider: RequestCodeProvider) {
        val processorUnit = findAndValidateProcessorUnit(kotlinProcessorUnits, element)
        val kotlinFile = processorUnit.createFile(rpe, requestCodeProvider)
        kotlinFile.writeTo(filer)
        println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> processKotlin")
//        throw Exception(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> processKotlin")
    }

    private fun processJava(element: Element, rpe: RuntimePermissionsElement, requestCodeProvider: RequestCodeProvider) {
        val processorUnit = findAndValidateProcessorUnit(javaProcessorUnits, element)
        val javaFile = processorUnit.createFile(rpe, requestCodeProvider)
        javaFile.writeTo(filer)
        println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> processJava")
//        throw Exception(">>>>>>>>>>>>>>>>>>>>>>>>>>>>> processJava")
    }
}
