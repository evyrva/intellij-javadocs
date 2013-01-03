package com.github.ideajavadocs.action;

import com.github.ideajavadocs.generator.JavaDocGenerator;
import com.github.ideajavadocs.generator.impl.ClassJavaDocGenerator;
import com.github.ideajavadocs.generator.impl.FieldJavaDocGenerator;
import com.github.ideajavadocs.generator.impl.MethodJavaDocGenerator;
import com.github.ideajavadocs.operation.JavaDocWriter;
import com.github.ideajavadocs.ui.component.JavaDocConfiguration;
import com.github.ideajavadocs.ui.dialog.JavaDocsDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

/**
 * The type Java docs generate action.
 *
 * @author Sergey Timofiychuk
 */
public class JavaDocsGenerateAction extends AnAction {

    private JavaDocWriter writer;
    private JavaDocConfiguration configuration;

    /**
     * Instantiates a new Java docs generate action.
     */
    public JavaDocsGenerateAction() {
        writer = ServiceManager.getService(JavaDocWriter.class);
    }

    /**
     * Action performed.
     *
     * @param e the Event
     */
    public void actionPerformed(AnActionEvent e) {
        final PsiFile file = DataKeys.PSI_FILE.getData(e.getDataContext());
        if (file == null) {
            // TODO show message
            return;
        }
        JavaDocsDialog dialog = new JavaDocsDialog(file.getProject(), getConfiguration(file.getProject()),
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<PsiElement> elements = new LinkedList<PsiElement>();
                // Find all class elements
                List<PsiClass> classElements = getClasses(file);
                elements.addAll(classElements);
                for (PsiClass classElement : classElements) {
                    elements.addAll(PsiTreeUtil.getChildrenOfTypeAsList(classElement, PsiMethod.class));
                    elements.addAll(PsiTreeUtil.getChildrenOfTypeAsList(classElement, PsiField.class));
                }
                for (PsiElement element : elements) {
                    processElement(element);
                }
            }
        });
        dialog.show();
        if (!dialog.isOK()) {
            // TODO Cancel handling
            return;
        }
        // TODO perform action
    }

    /**
     * Gets the classes.
     *
     * @param element the Element
     * @return the Classes
     */
    private List<PsiClass> getClasses(PsiElement element) {
        List<PsiClass> elements = new LinkedList<PsiClass>();
        List<PsiClass> classElements = PsiTreeUtil.getChildrenOfTypeAsList(element, PsiClass.class);
        elements.addAll(classElements);
        for (PsiClass classElement : classElements) {
            elements.addAll(getClasses(classElement));
        }
        return elements;
    }

    /**
     * Gets the generator.
     *
     * @param element the Element
     * @return the Generator
     */
    @Nullable
    private JavaDocGenerator getGenerator(@NotNull PsiElement element) {
        Project project = element.getProject();
        JavaDocGenerator generator = null;
        if (PsiClass.class.isAssignableFrom(element.getClass())) {
            generator = new ClassJavaDocGenerator(project);
        } else if (PsiMethod.class.isAssignableFrom(element.getClass())) {
            generator = new MethodJavaDocGenerator(project);
        } else if (PsiField.class.isAssignableFrom(element.getClass())) {
            generator = new FieldJavaDocGenerator(project);
        }
        return generator;
    }

    /**
     * Process element.
     *
     * @param element the Element
     */
    private void processElement(@NotNull PsiElement element) {
        JavaDocGenerator generator = getGenerator(element);
        if (generator != null) {
            @SuppressWarnings("unchecked")
            PsiDocComment javaDoc = generator.generate(element);
            if (javaDoc != null) {
                writer.write(javaDoc, element);
            }
        }
    }

    private JavaDocConfiguration getConfiguration(Project project) {
        if (configuration == null) {
            configuration = ServiceManager.getService(project, JavaDocConfiguration.class);
        }
        return configuration;
    }
}
