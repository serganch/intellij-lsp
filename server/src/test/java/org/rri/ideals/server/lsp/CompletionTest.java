package org.rri.ideals.server.lsp;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.intellij.openapi.util.Ref;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.rri.ideals.server.TestUtil;
import org.rri.ideals.server.completions.CompletionServiceTestUtil;
import org.rri.ideals.server.completions.engines.CompletionTestGenerator;
import org.rri.ideals.server.engine.DefaultTestFixture;
import org.rri.ideals.server.engine.TestEngine;
import org.rri.ideals.server.generator.IdeaOffsetPositionConverter;
import org.rri.ideals.server.util.MiscUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CompletionTest extends LspServerTestBase {

  @Override
  protected String getProjectRelativePath() {
    return "completion/integration-test";
  }

  @Test
  public void completion() {
    final String label = "completionVariant";

    final Set<CompletionItem> expectedCompletionList = Set.of(
        CompletionServiceTestUtil.createCompletionItem(
            label,
            "()",
            "int",
            new ArrayList<>(),
            label,
            CompletionItemKind.Method
        ),
        CompletionServiceTestUtil.createCompletionItem(
            label,
            "(int x)",
            "void",
            new ArrayList<>(),
            label,
            CompletionItemKind.Method)
    );
    CompletionTestGenerator.CompletionTest test;
    try {
      assertNotNull(server().getProject());
      var engine = new TestEngine(getProjectPath());
      engine.initSandbox(new DefaultTestFixture(getTestDataRoot().resolve("sandbox"), getProjectPath()));
      CompletionTestGenerator generator = new CompletionTestGenerator(engine.textsByFile, engine.markersByFile, new IdeaOffsetPositionConverter(server().getProject()));
      test = generator.generateTests().get(0);
    } catch (IOException e) {
      throw MiscUtil.wrap(e);
    }
    var params = test.params();
    Ref<Either<List<CompletionItem>, CompletionList>> completionResRef = new Ref<>();

    Assertions.assertDoesNotThrow(() -> completionResRef.set(
        TestUtil.getNonBlockingEdt(server().getTextDocumentService().completion(params), 3000)));
    var completionItemList = extractItemList(completionResRef.get());

    var itemForResolve = completionItemList
        .stream()
        .filter(completionItem ->
            completionItem.getLabelDetails().getDetail().equals("()"))
        .findFirst()
        .orElseThrow(() -> new AssertionError("item wasn't found"));
    var gson = new GsonBuilder().create();
    itemForResolve.setData(gson.fromJson(gson.toJson(itemForResolve.getData()), JsonObject.class));
    var resolvedItem = TestUtil.getNonBlockingEdt(
        server()
            .getTextDocumentService()
            .resolveCompletionItem(
                itemForResolve
            ),
        3000);
    String originalText = test.getSourceText();
    var allEdits = new ArrayList<>(resolvedItem.getAdditionalTextEdits());
    allEdits.add(resolvedItem.getTextEdit().getLeft());

    var insertedText = TestUtil.applyEdits(originalText, allEdits);
    var expectedText = test.answer();
    assertNotNull(expectedText);
    Assertions.assertEquals(expectedText, insertedText);
    completionItemList.forEach(CompletionServiceTestUtil::removeResolveInfo);
    Assert.assertEquals(expectedCompletionList, new HashSet<>(completionItemList));
  }

  @NotNull
  private static List<CompletionItem> extractItemList(@NotNull Either<List<CompletionItem>, CompletionList> completionResult) {
    List<CompletionItem> completionItemList;
    if (completionResult.isRight()) {
      completionItemList = completionResult.getRight().getItems();
    } else {
      Assert.assertTrue(completionResult.isLeft());
      completionItemList = completionResult.getLeft();
    }
    return completionItemList;
  }
}
