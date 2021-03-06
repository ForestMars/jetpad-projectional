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
package jetbrains.jetpad.grammar.slr;

import jetbrains.jetpad.grammar.Rule;
import jetbrains.jetpad.grammar.Symbol;

import java.util.List;

class SLRItem {
  private Rule myRule;
  private int myIndex;

  SLRItem(Rule rule, int index) {
    if (index < 0 || index > rule.getSymbols().size()) throw new IllegalArgumentException();

    myRule = rule;
    myIndex = index;
  }

  Rule getRule() {
    return myRule;
  }

  int getIndex() {
    return myIndex;
  }

  boolean isKernel() {
    if (getRule().getHead() == getRule().getGrammar().getStart()) return true;
    return myIndex > 0;
  }

  boolean isInitial() {
    return myIndex == 0;
  }

  boolean isFinal() {
    return myIndex == myRule.getSymbols().size();
  }

  Symbol getNextSymbol() {
    if (isFinal()) return null;
    return myRule.getSymbols().get(myIndex);
  }

  SLRItem getNextItem() {
    if (getNextSymbol() == null) throw new IllegalStateException();
    return new SLRItem(myRule, myIndex + 1);
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SLRItem)) return false;

    SLRItem item = (SLRItem) obj;
    return item.myRule == myRule && item.myIndex == myIndex;
  }

  @Override
  public int hashCode() {
    return myRule.hashCode() * 31 + myIndex;
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append(myRule.getHead()).append(" ->");
    List<Symbol> symbols = myRule.getSymbols();
    if (symbols.isEmpty()) {
      result.append(" ").append(".<eps>");
    } else {
      for (int i = 0; i < symbols.size(); i++) {
        result.append(" ");
        if (myIndex == i) {
          result.append(".");
        }
        result.append(symbols.get(i));
      }
      if (myIndex == symbols.size()) {
        result.append(" .");
      }
    }
    return result.toString();
  }
}