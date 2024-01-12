package com.gmail.visualbukkit.blocks;

import com.gmail.visualbukkit.VisualBukkitApp;
import com.gmail.visualbukkit.blocks.parameters.BlockParameter;
import com.gmail.visualbukkit.project.BuildInfo;
import com.gmail.visualbukkit.ui.TextIconButton;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public sealed abstract class Block extends VBox permits PluginComponentBlock, StatementBlock, ExpressionBlock {

    public static final PseudoClass INVALID_STYLE_CLASS = PseudoClass.getPseudoClass("invalid");

    private final HBox header = new HBox();
    private final ContextMenu contextMenu = new ContextMenu();
    private GridPane parameterGrid;
    protected List<BlockParameter> parameters;

    private final BooleanProperty collapsed = new SimpleBooleanProperty(false);

    public Block() {
        getStyleClass().add("block");
        Label blockTitle = new Label(getDefinition().name());
        blockTitle.getStyleClass().add("block-title");
        header.getStyleClass().add("block-header");
        header.getChildren().add(blockTitle);
        getChildren().add(header);
        setFocusTraversable(true);

        setOnContextMenuRequested(e -> {
            contextMenu.show(VisualBukkitApp.getPrimaryStage().getScene().getWindow(), e.getScreenX(), e.getScreenY());
            e.consume();
        });

        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                getPluginComponentBlock().updateState();
                requestFocus();
                e.consume();
            }
        });
    }

    public void addToHeader(Node node) {
        if (header.getChildren().size() == 1) {
            HBox spacer = new HBox();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            header.getChildren().add(spacer);
        }
        header.getChildren().add(node);
    }

    public <T extends Node & BlockParameter> void addParameter(String label, T parameter) {
        addParameter(label, null, parameter);
    }

    public <T extends Node & BlockParameter> void addParameter(String labelText, String tooltip, T parameter) {
        if (parameters == null) {
            TextIconButton toggleCollapseButton = new TextIconButton("▼", e -> collapsed.set(!isCollapsed()));
            toggleCollapseButton.textProperty().bind(Bindings.when(collapsed).then("▶").otherwise("▼"));
            addToHeader(toggleCollapseButton);
            getChildren().add(1, parameterGrid = new GridPane());
            parameterGrid.getStyleClass().add("parameter-grid");
            parameterGrid.visibleProperty().bind(collapsed.not());
            parameterGrid.managedProperty().bind(parameterGrid.visibleProperty());
            parameters = new ArrayList<>();
        }
        Label label = new Label(labelText + ": ");
        if (tooltip != null) {
            label.setTooltip(new Tooltip(tooltip));
        }
        parameters.add(parameter);
        parameterGrid.addRow(parameterGrid.getRowCount(), label, parameter);
    }

    public void removeParameters(int index) {
        parameterGrid.getChildren().removeIf(n -> GridPane.getRowIndex(n) >= index);
        while (index < parameters.size()) {
            parameters.remove(index);
        }
        if (parameters.isEmpty()) {
            parameters = null;
            getChildren().remove(parameterGrid);
            header.getChildren().remove(header.getChildren().size() - 1);
        }
    }

    public void updateState() {
        pseudoClassStateChanged(INVALID_STYLE_CLASS, false);
        if (parameters != null) {
            parameters.forEach(BlockParameter::updateState);
        }
    }

    public JSONObject serialize() {
        JSONObject json = new JSONObject();
        json.put("type", getDefinition().id());
        json.put("uuid", getUUID());
        if (parameters != null) {
            for (BlockParameter parameter : parameters) {
                json.append("params", parameter.serialize());
            }
        }
        if (isCollapsed()) {
            json.put("collapsed", true);
        }
        return json;
    }

    public void deserialize(JSONObject json) {
        setId(json.optString("uuid", null));
        JSONArray parameterJson = json.optJSONArray("params");
        if (parameterJson != null && parameters != null) {
            for (int i = 0; i < Math.min(parameters.size(), parameterJson.length()); i++) {
                parameters.get(i).deserialize(parameterJson.opt(i));
            }
        }
        if (json.optBoolean("collapsed")) {
            collapsed.set(true);
        }
    }

    public abstract void delete();

    public String arg(int i, BuildInfo buildInfo) {
        return parameters.get(i).generateJava(buildInfo);
    }

    public boolean isCollapsed() {
        return collapsed.get();
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed.set(collapsed);
    }

    public ContextMenu getContextMenu() {
        return contextMenu;
    }

    public BlockDefinition getDefinition() {
        return getClass().getAnnotation(BlockDefinition.class);
    }

    public String getUUID() {
        String id = getId();
        if (id == null) {
            setId(id = UUID.randomUUID().toString());
        }
        return id;
    }

    public PluginComponentBlock getPluginComponentBlock() {
        Parent parent = getParent();
        while (parent != null) {
            if (parent instanceof PluginComponentBlock b) {
                return b;
            }
            if (parent instanceof StatementHolder s && s.getOwner() instanceof PluginComponentBlock b) {
                return b;
            }
            parent = parent.getParent();
        }
        return null;
    }
}