/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.ide.macro;

import com.intellij.ide.IdeCoreBundle;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.BaseProjectDirectories;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class FileDirRelativeToProjectRootMacro extends Macro {
  @NotNull
  @Override
  public String getName() {
    return "FileDirRelativeToProjectRoot";
  }

  @NotNull
  @Override
  public String getDescription() {
    return IdeCoreBundle.message("macro.file.dir.relative.to.root");
  }

  @Override
  public String expand(@NotNull final DataContext dataContext) {
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) {
      return null;
    }
    VirtualFile file = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
    if (file == null) {
      return null;
    }
    if (!file.isDirectory()) {
      file = file.getParent();
      if (file == null) {
        return null;
      }
    }

    VirtualFile contentRoot = ProjectRootManager.getInstance(project).getFileIndex().getContentRootForFile(file);
    if (contentRoot != null && contentRoot.isDirectory()) {
      return FileUtil.getRelativePath(getIOFile(contentRoot), getIOFile(file));
    }

    final VirtualFile baseDirectory = BaseProjectDirectories.getInstance(project).getBaseDirectoryFor(file);
    if (baseDirectory != null) {
      return FileUtil.getRelativePath(getIOFile(baseDirectory), getIOFile(file));
    }

    return null;
  }
}
