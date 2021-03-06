/*
 * Copyright 2012-2013 JetBrains s.r.o
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrains.jetpad.projectional.testApp.mapper;

import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.cell.Cell;
import jetbrains.jetpad.cell.action.CellAction;
import jetbrains.jetpad.cell.completion.CompletionItem;
import jetbrains.jetpad.cell.completion.CompletionParameters;
import jetbrains.jetpad.cell.completion.SimpleCompletionItem;
import jetbrains.jetpad.projectional.cell.Role;
import jetbrains.jetpad.projectional.cell.RoleCompletion;
import jetbrains.jetpad.projectional.testApp.model.*;

import java.util.ArrayList;
import java.util.List;

class ExprCompletion implements RoleCompletion<ExprNode, Expr> {
  @Override
  public List<CompletionItem> createRoleCompletion(CompletionParameters ctx, Mapper<?, ? extends Cell> mapper, ExprNode contextNode, final Role<Expr> target) {
    List<CompletionItem> result = new ArrayList<CompletionItem>();
    result.add(new SimpleCompletionItem("id") {
      @Override
      public CellAction complete(String text) {
        return target.set(new IdExpr());
      }
    });
    result.add(new SimpleCompletionItem("i") {
      @Override
      public CellAction complete(String text) {
        return target.set(new IdExpr());
      }
    });
    result.add(new SimpleCompletionItem("+") {
      @Override
      public CellAction complete(String text) {
        return target.set(new PlusExpr());
      }
    });
    result.add(new SimpleCompletionItem("-") {
      @Override
      public CellAction complete(String text) {
        return target.set(new MinusExpr());
      }
    });
    return result;
  }
}