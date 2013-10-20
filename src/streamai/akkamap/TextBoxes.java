package streamai.akkamap;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.execution.junit.JUnitUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.Query;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Created by Nathan on 13/10/13.
 */

public class TextBoxes extends AnAction {

    private static final Logger LOG = Logger.getInstance("#com.conductor.checktests.TestClassDetectorImpl");


    final static String MESSAGE_DISPOSED = "Check for Tests can't be performed while the project is disposed doing other work";
    final static String TITLE_DISPOSED = "Check For Tests Is not Possible Right Now";
    final static String MESSAGE_DUMB = "Check for Tests can't be performed while IntelliJ IDEA updates the indices in background.\n"
            + "You can commit the changes without running inspections, or you can wait until indices are built.";
    final static String TITLE_DUMB = "Check for Tests is not possible right now";

    // If you register the action from Java code, this constructor is used to set the menu item name
    // (optionally, you can specify the menu description and an icon to display next to the menu item).
    // You can omit this constructor when registering the action in the plugin.xml file.
    public TextBoxes() {
        // Set the menu item name.
        super("Text _Boxes");
        // Set the menu item name, description and icon.
        // super("Text _Boxes","Item description",IconLoader.getIcon("/Mypackage/icon.png"));
    }

    public void actionPerformed(AnActionEvent event) {
        final DataContext dataContext = event.getDataContext();
        final Project project = event.getData(PlatformDataKeys.PROJECT);

        if (project == null) {
            throw new IllegalStateException("project is null");
        }

        if (project.isDisposed()) {
            showMessageDialog(project, MESSAGE_DISPOSED, TITLE_DISPOSED);
            return;
        }

        if (projectIsDumb(project)) {
            showMessageDialog(project, MESSAGE_DUMB, TITLE_DUMB);
            return;
        }

        JavaPsiFacade.getInstance(project).findClass("akka.actor.ActorSystem", GlobalSearchScope.allScope(project));


        PsiClass actorClass = JavaPsiFacade.getInstance(project).findClass("akka.actor.UntypedActor", GlobalSearchScope.allScope(project));


        PsiMethod[] actorOfs = JavaPsiFacade.getInstance(project).findClass("akka.actor.ActorRefFactory", GlobalSearchScope.allScope(project)).findMethodsByName("actorOf", false);

        Query<PsiReference> search = MethodReferencesSearch.search(actorOfs[0]);

        PsiUtils.getPsiClass(((PsiReferenceExpression)(MethodReferencesSearch.search(actorOfs[1]).findAll().toArray()[0])).getElement()).getNameIdentifier().getText(); //containing class




        (((PsiReferenceExpression)(MethodReferencesSearch.search(actorOfs[1]).findAll().toArray()[0])).getElement());

        PsiType psiType = ((PsiExpressionList) ((PsiMethodCallExpression) ((PsiReferenceExpression) (((PsiReferenceExpression) (MethodReferencesSearch.search(actorOfs[1]).findAll().toArray()[0])).getElement())).getContext()).getArgumentList().getExpressions()[0].getChildren()[1]).getExpressionTypes()[0];

        ((PsiExpressionList)((PsiMethodCallExpression)((PsiReferenceExpression)(((PsiReferenceExpression)(MethodReferencesSearch.search(actorOfs[1]).findAll().toArray()[0])).getElement())).getContext()).getArgumentList().getExpressions()[0].getChildren()[1]).getExpressionTypes();





        final GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
        AllClassesSearch.search(projectScope, project);

        String txt= Messages.showInputDialog(project, "What is your name?", "Input your name", Messages.getQuestionIcon());
        Messages.showMessageDialog(project, "Hello, " + txt + "!\n I am glad to see you.", "Information", Messages.getInformationIcon());

        checkTests(event, project);
    }

    // VisibleForTesting
    void checkTests(final AnActionEvent event, final Project project) {
        final VirtualFile[] virtualFiles = event.getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        if (virtualFiles != null && virtualFiles.length > 0) {
            final Set<PsiClass> testClasses = getTestClasses(project, virtualFiles);
            if(testClasses != null) {
                showTestListDialog(project, Lists.newArrayList(testClasses));
            }
        }
    }

    // VisibleForTesting
    Set<PsiClass> getTestClasses(Project project, VirtualFile[] virtualFiles) {
        return findTestClasses(Lists.newArrayList(virtualFiles), 10, project);
    }




    public Set<PsiClass> findTestClasses(final List<VirtualFile> virtualFiles, final int levelsToSearch, final Project myProject) throws ProcessCanceledException {
        final Set<PsiClass> result = Sets.newHashSet();
        boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
            @Override
            public void run() {
                try {
                    @Nullable
                    final ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
                    progress.setText("Processing");
                    progress.setIndeterminate(true);
                    final LinkedList<PsiElement> referenceSearchElements = Lists.newLinkedList();
                    for(final VirtualFile virtualFile : virtualFiles) {
                        if (progress.isCanceled()) {
                            throw new ProcessCanceledException();
                        }
                        referenceSearchElements.addAll(getReferenceSearchElements(virtualFile, myProject));
                    }
                    result.addAll(findTestClasses(referenceSearchElements, levelsToSearch));
                } catch (Exception e) {
                    LOG.error(e);
                }
            }
        }, "Checking for Tests", true, myProject);

        if (!completed) {
            throw new ProcessCanceledException();
        }

        return result;
    }


    Set<PsiElement> getReferenceSearchElements(final VirtualFile virtualFile, Project myProject) {
        final Set<PsiElement> elements = Sets.newHashSet();
        if(virtualFile != null) {
            final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(virtualFile);
            final PsiClass[] psiClasses = PsiUtils.getPsiClasses(psiFile);
            if(psiClasses != null) {
                elements.addAll(Arrays.asList(psiClasses));
            } else {
                elements.add(psiFile);
            }
        }
        return elements;
    }




    // VisibleForTesting
    Set<PsiClass> findTestClasses(final LinkedList<PsiElement> psiElementsToSearch, final int levelsToSearch) {
        final Set<PsiClass> testClasses = new HashSet<PsiClass>();
        int currentSearchLevel = 1;
        int lastIndexForCurrentSearchLevel = psiElementsToSearch.size() - 1;
        for(int idx = 0; idx < psiElementsToSearch.size(); idx++) {
            if(idx > lastIndexForCurrentSearchLevel) {
                currentSearchLevel++;
                lastIndexForCurrentSearchLevel = psiElementsToSearch.size() - 1;
            }
            if(currentSearchLevel > levelsToSearch && levelsToSearch != 0) {
                break;
            }
            final PsiElement psiElementToSearch = psiElementsToSearch.get(idx);
            final List<PsiReference> psiReferences = new ArrayList<PsiReference>();

            final GlobalSearchScope projectScope = GlobalSearchScope.projectScope(psiElementToSearch.getProject());
            final PsiReference[] references = ReferencesSearch.search(psiElementToSearch, projectScope, false).toArray(
                    new PsiReference[0]);
            psiReferences.addAll(Lists.newArrayList(references));

            for (final PsiReference psiReference : psiReferences) {
                final PsiElement referenceElement = psiReference.getElement();
               final PsiClass referencePsiClass = PsiUtils.getPsiClass(referenceElement);
                if (referencePsiClass != null) {
                    if(!psiElementsToSearch.contains(referencePsiClass)) {
                        psiElementsToSearch.addLast(referencePsiClass);
                    }
                    if(isTestClass(referencePsiClass)) {
                        testClasses.add(referencePsiClass);
                    }
                } else {
                    final PsiFile psiFile = referenceElement.getContainingFile();
                    if(!psiElementsToSearch.contains(psiFile)) {
                        psiElementsToSearch.addLast(psiFile);
                    }
                }
            }
        }
        return testClasses;
    }

    /**
     * Will add the file psiFile to the testClassFile List. if a PsiFile with the same name already exists then it just
     * returns
     *
     * @param newPsiClass
     *            the class to add
     */
    private boolean isTestClass(final PsiClass newPsiClass) {
        return JUnitUtil.isTestClass(newPsiClass);
    }



    // VisibleForTesting
    boolean projectIsDumb(Project project) {
        return DumbService.getInstance(project).isDumb();
    }

    // VisibleForTesting
    void showMessageDialog(final Project project, final String message, final String title) {
        Messages.showMessageDialog(project, message, title, null);
    }

    void showTestListDialog(final Project project, final List<PsiClass> testClasses) {
        showDialog(project, testClasses);
    }

    // VisibleForTesting
    static void showDialog(final Project project, final List<PsiClass> testClasses) {
        final DialogBuilder dialogBuilder = new DialogBuilder(project);
        dialogBuilder.setTitle("CheckTests Results");
        final JTextArea textArea = new JTextArea(10, 50);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        if (testClasses.size() > 0) {
            final List<String> lines = new ArrayList<String>();
            lines.add("Found " + testClasses.size() + " tests, would you like to run them?");
            for (final PsiClass testClass : testClasses) {
                lines.add(testClass.getName());
            }
            textArea.setText(StringUtil.join(lines, "\n"));
            final Runnable runTestsRunnable = new Runnable() {
                @Override
                public void run() {
                    dialogBuilder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
                    //TestRunner.runTest(project, testClasses);
                }
            };
            dialogBuilder.setOkOperation(runTestsRunnable);
        } else {
            textArea.setText("Found no tests, maybe you should create one");
        }
        dialogBuilder.setCenterPanel(ScrollPaneFactory.createScrollPane(textArea));
        dialogBuilder.show();
    }



}