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
import com.google.gwt.dom.client.Style;
import jetbrains.jetpad.base.Value;
import jetbrains.jetpad.geometry.Rectangle;
import jetbrains.jetpad.mapper.Mapper;
import jetbrains.jetpad.mapper.Synchronizers;
import jetbrains.jetpad.model.event.EventHandler;
import jetbrains.jetpad.model.event.Registration;
import jetbrains.jetpad.model.property.DerivedProperty;
import jetbrains.jetpad.model.property.ReadableProperty;
import jetbrains.jetpad.model.property.WritableProperty;
import jetbrains.jetpad.projectional.view.View;
import jetbrains.jetpad.values.Color;

class BaseViewMapper<ViewT extends View, ElementT extends Element> extends Mapper<ViewT, ElementT> {
  private View2DomContext myContext;

  BaseViewMapper(View2DomContext ctx, ViewT source, ElementT target) {
    super(source, target);
    myContext = ctx;
  }

  View2DomContext context() {
    return myContext;
  }

  protected boolean isDomLayout() {
    return false;
  }

  protected final boolean isDomPosition() {
    if (!(getParent() instanceof BaseViewMapper)) return false;
    return ((BaseViewMapper) getParent()).isDomLayout();
  }

  protected void whenValid(final Runnable r) {
    if (getSource().container() == null) {
      final Value<Registration> reg = new Value<Registration>();
      reg.set(getSource().attachEvents().addHandler(new EventHandler<Object>() {
        @Override
        public void onEvent(Object event) {
          whenValid(r);
          reg.get().remove();
        }
      }));
    } else {
      getSource().container().whenValid(r);
    }
  }

  @Override
  protected void registerSynchronizers(SynchronizersConfiguration conf) {
    super.registerSynchronizers(conf);

    Style targetStyle = getTarget().getStyle();

    if (!isDomPosition()) {
      targetStyle.setPosition(Style.Position.ABSOLUTE);
    } else {
      targetStyle.setPosition(Style.Position.RELATIVE);
    }

    if (!isDomPosition() || !isDomLayout()) {
      final ReadableProperty<Rectangle> positionInParent;
      if (getParent() instanceof BaseViewMapper) {
        final BaseViewMapper<?, ?> parent = (BaseViewMapper<?, ?>) getParent();
        positionInParent = new DerivedProperty<Rectangle>(getSource().bounds(), parent.getSource().bounds()) {
          @Override
          public Rectangle get() {
            Rectangle sourceBounds = getSource().bounds().get();
            Rectangle parentSourceBounds = parent.getSource().bounds().get();
            return sourceBounds.sub(parentSourceBounds.origin);
          }
        };
      } else {
        positionInParent = getSource().bounds();
      }

      final Value<Boolean> valid = new Value<Boolean>(false);
      conf.add(Synchronizers.forProperty(positionInParent, new WritableProperty<Rectangle>() {
        @Override
        public void set(final Rectangle value) {
          valid.set(false);
          whenValid(new Runnable() {
            @Override
            public void run() {
              if (valid.get()) return;
              final Rectangle value = positionInParent.get();
              Style style = getTarget().getStyle();

              if (!isDomPosition()) {
                style.setLeft(value.origin.x, Style.Unit.PX);
                style.setTop(value.origin.y, Style.Unit.PX);
              }

              if (!isDomLayout()) {
                style.setWidth(value.dimension.x, Style.Unit.PX);
                style.setHeight(value.dimension.y, Style.Unit.PX);
              }
              valid.set(true);
            }
          });
        }
      }));
    }

    conf.add(Synchronizers.forProperty(getSource().background(), new WritableProperty<Color>() {
      @Override
      public void set(Color value) {
        Style style = getTarget().getStyle();
        if (value == null) {
          style.setBackgroundColor(null);
        } else {
          style.setBackgroundColor(value.toCssColor());
        }
      }
    }));

    conf.add(Synchronizers.forProperty(getSource().border(), new WritableProperty<Color>() {
      @Override
      public void set(Color value) {
        Style style = getTarget().getStyle();
        if (value != null) {
          style.setBorderColor(value.toCssColor());
          style.setBorderWidth(1, Style.Unit.PX);
          style.setBorderStyle(Style.BorderStyle.SOLID);
        } else {
          style.clearBorderStyle();
          style.clearBorderColor();
          style.clearBorderWidth();
        }
      }
    }));

    conf.add(Synchronizers.forProperty(getSource().visible(), new WritableProperty<Boolean>() {
      @Override
      public void set(final Boolean value) {
        whenValid(new Runnable() {
          @Override
          public void run() {
            getTarget().getStyle().setDisplay(value ? Style.Display.BLOCK : Style.Display.NONE);
          }
        });
      }
    }));
  }
}