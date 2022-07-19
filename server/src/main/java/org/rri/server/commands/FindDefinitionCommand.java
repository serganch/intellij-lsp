package org.rri.server.commands;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.rri.server.util.MiscUtil;

import java.util.ArrayList;
import java.util.List;


public class FindDefinitionCommand extends MyCommand<Either<List<? extends Location>, List<? extends LocationLink>>> {
    @NonNull
    private final Position pos;

    public FindDefinitionCommand(@NotNull Position pos) {
        this.pos = pos;
    }

    @Override
    public @NonNull Either<List<? extends Location>, @NonNull List<@NonNull ? extends LocationLink>> apply(@NonNull ExecutorContext ctx) {
        PsiFile file = ctx.getPsiFile();
        Document doc = MiscUtil.getDocument(file);
        if (doc == null) { return Either.forRight(new ArrayList<>()); }

        int offset = MiscUtil.positionToOffset(pos, doc);
        PsiElement originalElem = file.findElementAt(offset);
        Range originalRange = MiscUtil.getPsiElementRange(originalElem, doc);

        PsiReference ref = file.findReferenceAt(offset);
        if (ref == null) { return Either.forRight(new ArrayList<>()); }

        PsiElement targetElem = ref.resolve();
        if (targetElem == null) { return Either.forRight(new ArrayList<>()); }

        Document targetDoc = targetElem.getContainingFile().equals(file)
                ? doc : MiscUtil.getDocument(targetElem.getContainingFile());
        LocationLink loc = MiscUtil.psiElementToLocationLink(targetElem, targetDoc, originalRange);
        List<LocationLink> lst = new ArrayList<>();
        lst.add(loc);
        return Either.forRight(lst);
    }
}
