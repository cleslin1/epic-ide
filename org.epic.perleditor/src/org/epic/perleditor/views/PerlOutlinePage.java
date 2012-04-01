package org.epic.perleditor.views;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.epic.core.ResourceMessages;
import org.epic.core.model.SourceFile;
import org.epic.core.model.Subroutine;
import org.epic.perleditor.PerlEditorPlugin;
import org.epic.perleditor.PerlPluginImages;
import org.epic.perleditor.preferences.PreferenceConstants;

public class PerlOutlinePage extends ContentOutlinePage
{
    private SourceFile source;
    
    /**
     * Subroutine in which the caret was during last call to updateSelection
     * We keep track of it to speed up outline synchronisations in the common
     * case (caret movements within a sub).
     */
    private Subroutine lastCaretSub;
    
    public PerlOutlinePage(SourceFile source)
    {
        this.source = source;
    }

    public void createControl(Composite parent)
    {
        super.createControl(parent);

        TreeViewer viewer = getTreeViewer();
        viewer.setContentProvider(new PerlOutlineContentProvider());
        viewer.setLabelProvider(new PerlOutlineLabelProvider());
        if(PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.OUTLINE_SORT)){
            viewer.setSorter(new ViewerSorter());
        }
        viewer.setInput(source);
        correctViewerExpansion();
        
        IMenuManager menuMan = getSite().getActionBars().getMenuManager();
        menuMan.add(new RefreshAction());
        
        registerToolbarActions(this.getSite().getActionBars());
    }
    
    public void refresh()
    {
        if (source != null)
        {
            SourceFile sameSource = source;
            source = null;
            updateContent(sameSource);
            correctViewerExpansion();
        }
    }

    public void updateContent(SourceFile source)
    {
        lastCaretSub = null;
        if (!source.equals(this.source))
        {
            this.source = source;
            getTreeViewer().setInput(source);
        }
    }
    
    public void updateSelection(int caretLine)
    {
        // check lastCaretSub first to speed up things in the most common case
        if (lastCaretSub == null ||
            caretLine < lastCaretSub.getStartLine() ||
            caretLine > lastCaretSub.getEndLine())
        {
            lastCaretSub = null;
            for (Iterator i = source.getSubs(); i.hasNext();)
            {
                Subroutine sub = (Subroutine) i.next();
                if (caretLine >= sub.getStartLine() &&
                    caretLine <= sub.getEndLine())
                {
                    lastCaretSub = sub;
                    break;
                }
            }
        }
        if (lastCaretSub != null)
            setSelection(new StructuredSelection(lastCaretSub));
        else
            setSelection(StructuredSelection.EMPTY);
    }
    
    public void correctViewerExpansion(){
        if(PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.OUTLINE_COLLAPSE_ALL)){
            getTreeViewer().collapseAll();
        }else{
            getTreeViewer().expandAll();
            try{
                TreeItem[] topLevelItems = getTreeViewer().getTree().getItems();
                for(int topIndex=0; topIndex < topLevelItems.length; topIndex++){
                    TreeItem[] items = topLevelItems[topIndex].getItems();
                    for(int itemsIndex=0; itemsIndex<items.length; itemsIndex++){
                        if(items[itemsIndex].getText().equals(PerlOutlineContentProvider.MODULES) &&
                                PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.OUTLINE_MODULE_FOLDING)){
                            items[itemsIndex].setExpanded(false);
                        }else if(items[itemsIndex].getText().equals(PerlOutlineContentProvider.SUBROUTINES) &&
                                PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.OUTLINE_SUBROUTINE_FOLDING)){
                            items[itemsIndex].setExpanded(false);
                        }
                        else{
                            items[itemsIndex].setExpanded(true);
                        }
                    }
                }
            }
            catch(IllegalArgumentException e){
                // Tree View is not available yet
            }
        }    
    }
    
    /**
     * This action is here as a fault tolerance measure - the outline
     * view may, unfortunately, become garbled due to some bug in EPIC.
     * To alleviate this problem somewhat, we give the user a way for
     * explicit recovery.
     */
    private class RefreshAction extends Action
    {
        public RefreshAction()
        {
            super();
            PlatformUI.getWorkbench().getHelpSystem().setHelp(this, ResourceMessages.getString("PerlOutlinePage.RefreshAction.label"));
            setText(ResourceMessages.getString("PerlOutlinePage.RefreshAction.label")); //$NON-NLS-1$
            setImageDescriptor(PerlPluginImages.getDescriptor(PerlPluginImages.IMG_ICON_OUTLINE_REFRESH));
            setToolTipText(ResourceMessages.getString("PerlOutlinePage.RefreshAction.tooltip")); //$NON-NLS-1$
            setDescription(ResourceMessages.getString("PerlOutlinePage.RefreshAction.descr")); //$NON-NLS-1$
        }
        
        public void run()
        {
            refresh();
        }
    }
    class LexicalSortingAction extends Action {
        public LexicalSortingAction() {
            super();
            PlatformUI.getWorkbench().getHelpSystem().setHelp(this, ResourceMessages.getString("PerlOutlinePage.RefreshAction.tooltip")); //$NON-NLS-1$
            setText(ResourceMessages.getString("PerlOutlinePage.LexicalSortAction.tooltip")); //$NON-NLS-1$
            setImageDescriptor(PerlPluginImages.getDescriptor(PerlPluginImages.IMG_ICON_OUTLINE_SORT));
            setToolTipText(ResourceMessages.getString("PerlOutlinePage.LexicalSortAction.tooltip")); //$NON-NLS-1$
            setDescription(ResourceMessages.getString("PerlOutlinePage.LexicalSortAction.descr")); //$NON-NLS-1$
        }
        public void run() {
            if(PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.OUTLINE_SORT)){
                getTreeViewer().setSorter(null);
                setChecked(true);
                PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.OUTLINE_SORT, false);
            }else{
                getTreeViewer().setSorter(new ViewerSorter());
                setChecked(false);
                PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.OUTLINE_SORT, true);
            }
        }
    }
    class CollapseAllAction extends Action{
        public CollapseAllAction(){
            super();
            PlatformUI.getWorkbench().getHelpSystem().setHelp(this, ResourceMessages.getString("PerlOutlinePage.CollapseAllAction.tooltip"));
            setText(ResourceMessages.getString("PerlOutlinePage.CollapseAllAction.tooltip"));
            setImageDescriptor(PerlPluginImages.getDescriptor(PerlPluginImages.IMG_ICON_OUTLINE_COLLAPSE));
            setToolTipText(ResourceMessages.getString("PerlOutlinePage.CollapseAllAction.tooltip")); //$NON-NLS-1$
            setDescription(ResourceMessages.getString("PerlOutlinePage.CollapseAllAction.descr")); //$NON-NLS-1$
        }
        public void run(){
            if(PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.OUTLINE_COLLAPSE_ALL)){
                PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.OUTLINE_COLLAPSE_ALL, false);
                PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.OUTLINE_SUBROUTINE_FOLDING, false);
                PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.OUTLINE_MODULE_FOLDING, false);
                setChecked(false);
            }else{
                PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.OUTLINE_COLLAPSE_ALL, true);
                PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.OUTLINE_SUBROUTINE_FOLDING, true);
                PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.OUTLINE_MODULE_FOLDING, true);
                setChecked(true);
            }
            correctViewerExpansion();
        }
    }
    class ShowModulesAction extends Action{
        public ShowModulesAction(){
            super();
            PlatformUI.getWorkbench().getHelpSystem().setHelp(this, ResourceMessages.getString("PerlOutlinePage.ShowModules.tooltip"));
            setText(ResourceMessages.getString("PerlOutlinePage.ShowModules.tooltip"));
            setImageDescriptor(PerlPluginImages.getDescriptor(PerlPluginImages.IMG_ICON_USE_NODE));
            setToolTipText(ResourceMessages.getString("PerlOutlinePage.ShowModules.tooltip")); //$NON-NLS-1$
            setDescription(ResourceMessages.getString("PerlOutlinePage.ShowModules.descr")); //$NON-NLS-1$
        }
        public void run(){
            if(PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.OUTLINE_MODULE_FOLDING)){
                PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.OUTLINE_MODULE_FOLDING, false);
                setChecked(false);
            }else{
                PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.OUTLINE_MODULE_FOLDING, true);
                setChecked(true);
            }
            correctViewerExpansion();
        }
    }
    class ShowSubroutineAction extends Action{
        public ShowSubroutineAction() {
            super();
            PlatformUI.getWorkbench().getHelpSystem().setHelp(this, ResourceMessages.getString("PerlOutlinePage.HideMethods.tooltip"));
            setText(ResourceMessages.getString("PerlOutlinePage.HideMethods.tooltip"));
            setImageDescriptor(PerlPluginImages.getDescriptor(PerlPluginImages.IMG_ICON_SUBROUTINE_NODE)); 
            setToolTipText(ResourceMessages.getString("PerlOutlinePage.HideMethods.tooltip")); //$$NON-NLS-1$
            setDescription(ResourceMessages.getString("PerlOutlinePage.HideMethods.descr")); //$$NON-NLS-1$
        }
        public void run(){
            if(PerlEditorPlugin.getDefault().getPreferenceStore().getBoolean(PreferenceConstants.OUTLINE_SUBROUTINE_FOLDING)){
                PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.OUTLINE_SUBROUTINE_FOLDING, false);
                setChecked(false);
            }else{
                PerlEditorPlugin.getDefault().getPreferenceStore().setValue(PreferenceConstants.OUTLINE_SUBROUTINE_FOLDING, true);
                setChecked(true);
            }
            correctViewerExpansion();
        }
    }
    
    private void registerToolbarActions(IActionBars actionBars){
        IToolBarManager toolBarManager = actionBars.getToolBarManager();
        toolBarManager.add(new CollapseAllAction());
        toolBarManager.add(new LexicalSortingAction());
        toolBarManager.add(new ShowModulesAction());
        toolBarManager.add(new ShowSubroutineAction());
    }
}
