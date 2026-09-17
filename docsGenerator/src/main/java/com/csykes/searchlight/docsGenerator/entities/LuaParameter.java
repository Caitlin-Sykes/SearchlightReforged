package com.csykes.searchlight.docsGenerator.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LuaParameter {
    String name;
    String type;
    String description;
}
