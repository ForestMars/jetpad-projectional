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
package jetbrains.jetpad.projectional.diagram.view;

import jetbrains.jetpad.base.Handler;
import jetbrains.jetpad.geometry.Rectangle;
import jetbrains.jetpad.geometry.Vector;
import jetbrains.jetpad.model.collections.CollectionItemEvent;
import jetbrains.jetpad.model.collections.CollectionListener;
import jetbrains.jetpad.model.event.EventHandler;
import jetbrains.jetpad.model.event.Registration;
import jetbrains.jetpad.model.property.Property;
import jetbrains.jetpad.model.property.PropertyChangeEvent;
import jetbrains.jetpad.projectional.diagram.base.GridDirection;
import jetbrains.jetpad.projectional.view.*;

import java.util.List;

public abstract class DiagramNodeView extends GroupView {
  private static final ViewPropertySpec<Integer> PADDING = new ViewPropertySpec<Integer>("padding", ViewPropertyKind.RELAYOUT, 20);
  private static final ViewPropertySpec<Vector> MINIMAL_SIZE = new ViewPropertySpec<Vector>("minimalSize", ViewPropertyKind.RELAYOUT, new Vector(20, 20));

  public final RectView rect = new RectView();
  public final GroupView inputs = new GroupView();
  public final GroupView outputs = new GroupView();

  public final View contentView = new GroupView();

  private GridDirection myPortsDirection = GridDirection.RIGHT;

  public DiagramNodeView() {
    children().add(rect);
    children().add(inputs);
    children().add(outputs);
    children().add(contentView);

    contentView.children().addListener(new CollectionListener<View>() {
      private Registration myReg = null;
      @Override
      public void onItemAdded(CollectionItemEvent<View> event) {
        if (contentView.children().size() > 1) {
          throw new IllegalStateException();
        }
        contentView.prop(DiagramViewSpecs.CONTENT_RECT_HANDLER).set(event.getItem().prop(DiagramViewSpecs.CONTENT_RECT_HANDLER).get());
        myReg = event.getItem().prop(DiagramViewSpecs.CONTENT_RECT_HANDLER).addHandler(new EventHandler<PropertyChangeEvent<Handler<Rectangle>>>() {
          @Override
          public void onEvent(PropertyChangeEvent<Handler<Rectangle>> event) {
            contentView.prop(DiagramViewSpecs.CONTENT_RECT_HANDLER).set(event.getNewValue());
          }
        });
      }

      @Override
      public void onItemRemoved(CollectionItemEvent<View> event) {
        myReg.remove();
      }
    });

    inputs.children().addListener(new CollectionListener<View>() {
      @Override
      public void onItemAdded(CollectionItemEvent<View> event) {
        event.getItem().prop(DiagramViewSpecs.CONNECTOR_DIR).set(myPortsDirection.opposite());
      }

      @Override
      public void onItemRemoved(CollectionItemEvent<View> event) {
        event.getItem().prop(DiagramViewSpecs.CONNECTOR_DIR).set(null);
      }
    });
    outputs.children().addListener(new CollectionListener<View>() {
      @Override
      public void onItemAdded(CollectionItemEvent<View> event) {
        event.getItem().prop(DiagramViewSpecs.CONNECTOR_DIR).set(myPortsDirection);
      }

      @Override
      public void onItemRemoved(CollectionItemEvent<View> event) {
        event.getItem().prop(DiagramViewSpecs.CONNECTOR_DIR).set(null);
      }
    });
  }

  public Property<Integer> padding() {
    return prop(PADDING);
  }

  public Property<Vector> minimalSize() {
    return prop(MINIMAL_SIZE);
  }

  public GridDirection getPortsDirection() {
    return myPortsDirection;
  }

  public void setPortsDirection(GridDirection dir) {
    myPortsDirection = dir;
    for (View input: inputs.children()) {
      input.prop(DiagramViewSpecs.CONNECTOR_DIR).set(myPortsDirection.opposite());
    }
    for (View output: outputs.children()) {
      output.prop(DiagramViewSpecs.CONNECTOR_DIR).set(myPortsDirection);
    }
    invalidate();
  }

  @Override
  protected void doValidate(ValidationContext ctx) {
    //calculate children bounds
    super.doValidate(ctx);

    rect.dimension().set(minimalSize().get());

    int padding = padding().get();

    Handler<Rectangle> dimensionSupplier = contentView.prop(DiagramViewSpecs.CONTENT_RECT_HANDLER).get();
    Vector paddingVector = new Vector(padding, padding);
    if (dimensionSupplier != null) {
      dimensionSupplier.handle(new Rectangle(rect.bounds().get().origin.add(paddingVector), rect.dimension().get().sub(paddingVector.mul(2))));
    } else {
      contentView.moveTo(rect.bounds().get().origin.add(paddingVector));
      Vector cellDimension = contentView.bounds().get().dimension.add(paddingVector.mul(2));
      rect.dimension().set(minimalSize().get().max(cellDimension));
    }

    layoutPorts(inputs.children(), myPortsDirection.opposite());
    layoutPorts(outputs.children(), myPortsDirection);

    //calculate block view bounds
    super.doValidate(ctx);
  }

  public abstract void layoutPorts(List<View> ports, GridDirection dir);
}