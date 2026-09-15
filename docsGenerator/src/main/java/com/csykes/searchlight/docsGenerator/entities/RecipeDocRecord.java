package com.csykes.searchlight.docsGenerator.entities;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RecipeDocRecord {
    private final String id;
    private final String type;
    private final String group;
    private final List<String> slots; // Exactly 9 elements (index 0 to 8 corresponding to slots 1 to 9)
    private final List<String> result;
    private final int resultCount;

    public String getSlot(int index) {
        if (slots != null && index >= 1 && index <= slots.size()) {
            return slots.get(index - 1);
        }
        return "";
    }

    public String toMarkdownTag() {
        StringBuilder sb = new StringBuilder("<RecipeGrid\n");
        for (int i = 0; i < 9; i++) {
            String slotVal = (slots != null && i < slots.size()) ? slots.get(i) : "";
            if (!slotVal.isEmpty()) {
                sb.append("  :slot").append(i + 1).append("=\"'").append(slotVal.replace("'", "\\'")).append("'\"\n");
            }
        }
        sb.append("/>");
        return sb.toString();
    }
}
