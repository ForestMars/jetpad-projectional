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
package jetbrains.jetpad.projectional.view.gwt;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.MapperFactory;
import jetbrains.jetpad.projectional.view.*;

class ViewMapperFactory {
  static MapperFactory<View, Element> factory(final View2DomContext ctx) {
    return new MapperFactory<View, Element>() {
      @Override
      public Mapper<? extends View, ? extends Element> createMapper(View source) {
        Mapper<? extends View, ? extends Element> result;
        if (source instanceof TextView) {
          result = new TextViewMapper(ctx, (TextView) source);
        } else if (source instanceof LineView) {
          result = new LineViewMapper(ctx, (LineView) source);
        } else if (source instanceof MultiPointView) {
          result = new MultiPointViewMapper(ctx, (MultiPointView) source);
        } else if (source.getClass() == VerticalView.class) {
          result = new VerticalViewMapper(ctx, (VerticalView) source);
        } else if (source.getClass() == HorizontalView.class) {
          result = new HorizontalViewMapper(ctx, (HorizontalView) source);
        } else {
          result = new CompositeViewMapper<View, Element>(ctx, source, DOM.createDiv());
        }

        if (source instanceof VerticalView) {
          result.getTarget().addClassName("V");
        }

        if (source instanceof HorizontalView) {
          result.getTarget().addClassName("H");
        }

        if (source instanceof TextView) {
          result.getTarget().addClassName("T");
        }

        return result;
      }
    };
  }
}