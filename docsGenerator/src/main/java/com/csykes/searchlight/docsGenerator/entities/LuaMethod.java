package com.csykes.searchlight.docsGenerator.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuaMethod {
    String name;
    String description;
    List<LuaParameter> parameters;
}
