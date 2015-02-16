/**
 * Copyright (C) 2013-2014 all@code-story.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package net.codestory.http.templating;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

import net.codestory.http.compilers.*;
import net.codestory.http.io.*;
import net.codestory.http.markdown.*;
import net.codestory.http.misc.*;

import com.github.jknack.handlebars.*;

public class HandlebarsViewCompiler implements ViewCompiler {
  private final Resources resources;
  private final HandlebarsCompiler handlebars;

  public HandlebarsViewCompiler(Env env, Resources resources, CompilerFacade compilers) {
    this.resources = resources;
    this.handlebars = new HandlebarsCompiler(env, resources, compilers);
  }

  @Override
  public void configureHandlebars(Consumer<Handlebars> action) {
    handlebars.configure(action);
  }

  @Override
  public void addHandlebarsResolver(ValueResolver resolver) {
    handlebars.addResolver(resolver);
  }

  @Override
  public String render(String uri, Map<String, ?> keyValues) {
    Path path = resources.findExistingPath(uri);
    if (path == null) {
      throw new IllegalArgumentException("Template not found " + uri);
    }

    try {
      YamlFrontMatter yamlFrontMatter = YamlFrontMatter.parse(resources.sourceFile(path));

      String content = yamlFrontMatter.getContent();
      Map<String, Object> variables = yamlFrontMatter.getVariables();
      Map<String, Object> allKeyValues = merge(variables, keyValues);

      String body = handlebars.compile(content, allKeyValues);
      if (MarkdownCompiler.supports(path)) {
        body = MarkdownCompiler.INSTANCE.compile(body);
      }

      String layout = (String) variables.get("layout");
      if (layout == null) {
        return body;
      }

      String layoutContent = render("_layouts/" + layout, allKeyValues);
      return layoutContent.replace("[[body]]", body);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to render template", e);
    }
  }

  private static Map<String, Object> merge(Map<String, ?> first, Map<String, ?> second) {
    Map<String, Object> merged = new HashMap<>();
    merged.putAll(first);
    merged.putAll(second);
    merged.put("body", "[[body]]");
    return merged;
  }
}
