//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications.Bus;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.PsiTreeUtil;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConvertAction extends AnAction {

    private static final NotificationGroup GROUP_DISPLAY_ID_INFO;

    static {
        GROUP_DISPLAY_ID_INFO = new NotificationGroup("POJOtoText.Group", NotificationDisplayType.STICKY_BALLOON, true);
    }

    public void actionPerformed(AnActionEvent e) {
        Editor editor = e.getDataContext().getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getDataContext().getData(CommonDataKeys.PSI_FILE);
        Project project = editor.getProject();
        PsiElement referenceAt = psiFile.findElementAt(editor.getCaretModel().getOffset());
        PsiClass hostClass = (PsiClass) PsiTreeUtil.getContextOfType(referenceAt, new Class[]{PsiClass.class});
        PsiClass selectedClass;
        if (hostClass.getName() != null) {
            selectedClass = hostClass;
        } else {
            selectedClass = this.detectCorrectClassByName(referenceAt.getText(), hostClass, project);
        }

        if (selectedClass == null) {
            Notification notification = GROUP_DISPLAY_ID_INFO.createNotification("Selection is not a POJO.", NotificationType.ERROR);
            Bus.notify(notification, project);
        } else {
            try {

                String text = selectedClass.getName() + "\n" + formatFieldList(generateFieldList(selectedClass));
                StringSelection selection = new StringSelection(text);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
                String message = "Convert " + selectedClass.getName() + " to Text success, copied to the clipboard.";
                Notification notification = GROUP_DISPLAY_ID_INFO.createNotification(message, NotificationType.INFORMATION);
                Bus.notify(notification, project);
            } catch (Exception var15) {
                Notification notification = GROUP_DISPLAY_ID_INFO.createNotification("Convert to Text failed.", NotificationType.ERROR);
                Bus.notify(notification, project);
            }

        }
    }

    private List<String> generateFieldList(PsiClass psiClass) {
        PsiField[] psiFields = psiClass.getFields();
        List<String> list = new ArrayList<>(psiFields.length);
        for (PsiField psiField : psiFields) {
            list.add(psiField.getName());
        }
        return list;
    }

    private String formatFieldList(List<String> list) {
        StringBuilder sb = new StringBuilder();
        int size = list.size();
        for (int i = 0; i < size - 1; i++) {
            sb.append(list.get(i));//todo .append('\n');
        }
        if (list.size() > 0) {
            sb.append(list.get(size - 1));
        }
        return sb.toString();
    }

    private Map<String, Object> generateMap(PsiClass psiClass, Project project) {
        Map<String, Object> outputMap = new LinkedHashMap<>();
        PsiField[] psiFields = psiClass.getFields();

        for (PsiField psiField : psiFields) {
            outputMap.put(psiField.getName(), this.getObjectForField(psiField, project));
        }

        return outputMap;
    }

    private Object getObjectForField(PsiField psiField, Project project) {
        PsiType type = psiField.getType();
        if (type instanceof PsiPrimitiveType) {
            if (type.equals(PsiType.INT)) {
                return 0;
            } else if (type.equals(PsiType.BOOLEAN)) {
                return Boolean.TRUE;
            } else if (type.equals(PsiType.BYTE)) {
                return Byte.valueOf("1");
            } else if (type.equals(PsiType.CHAR)) {
                return '-';
            } else if (type.equals(PsiType.DOUBLE)) {
                return 0.0D;
            } else if (type.equals(PsiType.FLOAT)) {
                return 0.0F;
            } else if (type.equals(PsiType.LONG)) {
                return 0L;
            } else {
                return type.equals(PsiType.SHORT) ? Short.valueOf("0") : type.getPresentableText();
            }
        } else {
            String typeName = type.getPresentableText();
            if (!typeName.equals("Integer") && !typeName.equals("Long")) {
                if (!typeName.equals("Double") && !typeName.equals("Float")) {
                    if (typeName.equals("Boolean")) {
                        return Boolean.TRUE;
                    } else if (typeName.equals("Byte")) {
                        return Byte.valueOf("1");
                    } else if (typeName.equals("String")) {
                        return "str";
                    } else if (typeName.equals("Date")) {
                        return (new Date()).getTime();
                    } else if (typeName.startsWith("List")) {
                        return this.handleList(type, project, psiField.getContainingClass());
                    } else {
                        PsiClass fieldClass = this.detectCorrectClassByName(typeName, psiField.getContainingClass(), project);
                        return fieldClass != null ? this.generateMap(fieldClass, project) : typeName;
                    }
                } else {
                    return 0.0F;
                }
            } else {
                return 0;
            }
        }
    }

    private Object handleList(PsiType psiType, Project project, PsiClass containingClass) {
        List<Object> list = new ArrayList<>();
        PsiClassType classType = (PsiClassType) psiType;
        PsiType[] subTypes = classType.getParameters();
        if (subTypes.length > 0) {
            PsiType subType = subTypes[0];
            String subTypeName = subType.getPresentableText();
            if (subTypeName.startsWith("List")) {
                list.add(this.handleList(subType, project, containingClass));
            } else {
                PsiClass targetClass = this.detectCorrectClassByName(subTypeName, containingClass, project);
                if (targetClass != null) {
                    list.add(this.generateMap(targetClass, project));
                } else if (subTypeName.equals("String")) {
                    list.add("str");
                } else if (subTypeName.equals("Date")) {
                    list.add((new Date()).getTime());
                } else {
                    list.add(subTypeName);
                }
            }
        }

        return list;
    }

    private PsiClass detectCorrectClassByName(String className, PsiClass containingClass, Project project) {
        PsiClass[] classes = PsiShortNamesCache.getInstance(project).getClassesByName(className, GlobalSearchScope.projectScope(project));
        if (classes.length == 0) {
            return null;
        } else if (classes.length == 1) {
            return classes[0];
        } else {
            PsiJavaFile javaFile = (PsiJavaFile) containingClass.getContainingFile();
            PsiImportList importList = javaFile.getImportList();
            PsiImportStatement[] statements = importList.getImportStatements();
            Set<String> importedPackageSet = new HashSet<>();

            int idx;
            for (idx = 0; idx < statements.length; ++idx) {
                importedPackageSet.add(statements[idx].getQualifiedName());
            }

            for (idx = 0; idx < classes.length; ++idx) {
                PsiClass targetClass = classes[idx];
                PsiJavaFile targetClassContainingFile = (PsiJavaFile) targetClass.getContainingFile();
                String packageName = targetClassContainingFile.getPackageName();
                if (importedPackageSet.contains(packageName + "." + targetClass.getName())) {
                    return targetClass;
                }
            }

            return null;
        }
    }
}
