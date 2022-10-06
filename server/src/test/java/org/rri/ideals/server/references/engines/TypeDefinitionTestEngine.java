package org.rri.ideals.server.references.engines;

import com.intellij.openapi.project.Project;
import org.eclipse.lsp4j.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class TypeDefinitionTestEngine extends ReferencesTestEngineBase<TypeDefinitionTestEngine.TypeDefinitionTest> {
  public static class TypeDefinitionTest extends ReferencesTestEngineBase.ReferencesTestBase {
    private final TypeDefinitionParams params;

    private TypeDefinitionTest(TypeDefinitionParams params, List<? extends LocationLink> answer) {
      super(answer);
      this.params = params;
    }

    @Override
    public TypeDefinitionParams getParams() {
      return params;
    }
  }

  public TypeDefinitionTestEngine(Path directoryPath, Project project) throws IOException {
    super(directoryPath, project);
  }
  protected TypeDefinitionTest createReferencesTest(String uri, Position pos, List<LocationLink> locLinks) {
    return new TypeDefinitionTest(new TypeDefinitionParams(new TextDocumentIdentifier(uri), pos), locLinks);
  }
}
