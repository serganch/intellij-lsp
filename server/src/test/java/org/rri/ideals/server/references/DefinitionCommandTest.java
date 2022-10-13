package org.rri.ideals.server.references;

import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.rri.ideals.server.LspPath;
import org.rri.ideals.server.TestUtil;
import org.rri.ideals.server.engine.IdeaTestFixture;
import org.rri.ideals.server.engine.TestEngine;
import org.rri.ideals.server.generator.IdeaOffsetPositionConverter;
import org.rri.ideals.server.references.engines.DefinitionTestGenerator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;


@RunWith(JUnit4.class)
public class DefinitionCommandTest extends ReferencesCommandTestBase {
  @Test
  public void newDefinitionJavaTest() {
    final var dirPath = Paths.get(getTestDataPath(), "java/project-definition");
    checkDefinitionByDirectory(dirPath);
  }

  @Test
  public void newDefinitionPythonTest() {
    final var dirPath = Paths.get(getTestDataPath(), "python/project-definition");
    checkDefinitionByDirectory(dirPath);
  }

  private void checkDefinitionByDirectory(Path dirPath) {
    try {
      final var engine = new TestEngine(dirPath);
      engine.initSandbox(new IdeaTestFixture(myFixture));
      final var generator = new DefinitionTestGenerator(engine.textsByFile, engine.markersByFile, new IdeaOffsetPositionConverter(getProject()));
      final var referencesTests = generator.generateTests();
      for (final var test : referencesTests) {
        final var params = test.params();
        final var answer = test.answer();

        final var path = LspPath.fromLspUri(params.getTextDocument().getUri());
        final var future = new FindDefinitionCommand(params.getPosition()).runAsync(getProject(), path);
        final var actual =
            Optional.ofNullable(TestUtil.getNonBlockingEdt(future, 50000)).map(Either::getRight);

        assertTrue(actual.isPresent());
        assertEquals(answer, actual.get());
      }
    } catch (IOException | RuntimeException e) {
      System.err.println(e instanceof IOException ? "IOException:" : "RuntimeException");
      System.err.println(e.getMessage());
      fail();
    }
  }
}
