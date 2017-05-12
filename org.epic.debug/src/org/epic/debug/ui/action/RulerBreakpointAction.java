/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.epic.debug.ui.action;

import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

/**
 * @deprecated direct cut and paste of org.eclipse.debug.ui.actions.RulerBreakpointAction to enable
 * support for 3.1 users. if/when 3.1 support is no longer required, this class can be removed.
 */
@Deprecated
public abstract class RulerBreakpointAction extends Action {

    private ITextEditor fEditor;
    private IVerticalRulerInfo fRulerInfo;

    /**
     * Constructs an action to work on breakpoints in the specified
     * text editor with the specified vertical ruler information.
     *
     * @param editor text editor
     * @param info vertical ruler information
     */
    public RulerBreakpointAction(ITextEditor editor, IVerticalRulerInfo info) {
        fEditor = editor;
        fRulerInfo = info;
    }

    /**
     * Returns the breakpoint at the last line of mouse activity in the ruler
     * or <code>null</code> if none.
     *
     * @return breakpoint associated with activity in the ruler or <code>null</code>
     */
    protected IBreakpoint getBreakpoint() {
        IAnnotationModel annotationModel = fEditor.getDocumentProvider().getAnnotationModel(fEditor.getEditorInput());
        IDocument document = fEditor.getDocumentProvider().getDocument(fEditor.getEditorInput());
        if (annotationModel != null) {
            Iterator<Annotation> iterator = annotationModel.getAnnotationIterator();
            while (iterator.hasNext()) {
                Object object = iterator.next();
                if (object instanceof SimpleMarkerAnnotation) {
                    SimpleMarkerAnnotation markerAnnotation = (SimpleMarkerAnnotation) object;
                    IMarker marker = markerAnnotation.getMarker();
                    try {
                        if (marker.isSubtypeOf(IBreakpoint.BREAKPOINT_MARKER)) {
                            Position position = annotationModel.getPosition(markerAnnotation);
                            int line = document.getLineOfOffset(position.getOffset());
                            if (line == fRulerInfo.getLineOfLastMouseButtonActivity()) {
                                IBreakpoint breakpoint = DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(marker);
                                if (breakpoint != null) {
                                    return breakpoint;
                                }
                            }
                        }
                    } catch (CoreException e) {
                    } catch (BadLocationException e) {
                    }
                }
            }
        }
        return null;
    }

    /**
     * Returns the editor this action was created for.
     *
     * @return editor
     */
    protected ITextEditor getEditor() {
        return fEditor;
    }

    /**
     * Returns the vertical ruler information this action was created for.
     *
     * @return vertical ruler information
     */
    protected IVerticalRulerInfo getVerticalRulerInfo() {
        return fRulerInfo;
    }

}
