package com.impactradar.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class TextDiffService {

    public DiffResult diff(String oldText, String newText) {
        List<String> oldLines = splitLines(oldText);
        List<String> newLines = splitLines(newText);

        int[][] lcs = buildLcsTable(oldLines, newLines);

        List<String> diffLines = new ArrayList<>();
        int added = 0;
        int removed = 0;

        int i = 0;
        int j = 0;

        while (i < oldLines.size() && j < newLines.size()) {
            if (oldLines.get(i).equals(newLines.get(j))) {
                diffLines.add("  " + oldLines.get(i));
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                diffLines.add("- " + oldLines.get(i));
                removed++;
                i++;
            } else {
                diffLines.add("+ " + newLines.get(j));
                added++;
                j++;
            }
        }

        while (i < oldLines.size()) {
            diffLines.add("- " + oldLines.get(i));
            removed++;
            i++;
        }

        while (j < newLines.size()) {
            diffLines.add("+ " + newLines.get(j));
            added++;
            j++;
        }

        return new DiffResult(
                String.join(System.lineSeparator(), diffLines),
                added,
                removed
        );
    }

    private int[][] buildLcsTable(
            List<String> oldLines,
            List<String> newLines
    ) {
        int[][] lcs = new int[oldLines.size() + 1][newLines.size() + 1];

        for (int i = oldLines.size() - 1; i >= 0; i--) {
            for (int j = newLines.size() - 1; j >= 0; j--) {
                if (oldLines.get(i).equals(newLines.get(j))) {
                    lcs[i][j] = 1 + lcs[i + 1][j + 1];
                } else {
                    lcs[i][j] = Math.max(
                            lcs[i + 1][j],
                            lcs[i][j + 1]
                    );
                }
            }
        }

        return lcs;
    }

    private List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        return List.of(text.split("\\R", -1));
    }

    public record DiffResult(
            String text,
            int linesAdded,
            int linesRemoved
    ) {
    }
}