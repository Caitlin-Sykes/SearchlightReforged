package com.csykes.searchlight.docsGenerator.entities;

import java.util.List;

public record PeripheralDocumentation(String description, List<LuaMethod> methods) {
}