package com.learn.grammar.leet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NSum {
    public static List<List<Integer>> nSum(int[] nums, int target, int n) {
        Arrays.sort(nums);
        List<List<Integer>> result = new ArrayList<>();
        findCombinations(nums, target, n, 0, new ArrayList<>(), result);
        return result;
    }

    private static void findCombinations(
            int[] nums, int remainingTarget, int remainingNumbers, int startIndex,
            List<Integer> currentCombination, List<List<Integer>> result
    ) {
        if(remainingNumbers == 0) {
            if(remainingTarget == 0) {
                result.add(new ArrayList<>(currentCombination));
            }
            return;
        }

        if (remainingNumbers * nums[startIndex] > remainingTarget ||
            remainingNumbers * nums[nums.length - 1] < remainingNumbers) {
            return;
        }

        for (int i = startIndex; i <= nums.length - remainingNumbers; i++) {
            if (i > startIndex && nums[i] == nums[i - 1]) {
                continue;
            }

            currentCombination.add(nums[i]);
            findCombinations(nums, remainingTarget - nums[i],
                    remainingNumbers - 1, i + 1, currentCombination, result);
            currentCombination.remove(currentCombination.size() - 1);
        }
    }

    public static void main(String[] args) {
        int[] nums = {1, 0, -1, 0, -2, 2};
        int target = 0;
        int n = 4;

        List<List<Integer>> result = nSum(nums, target, n);

        System.out.printf("""
            Input: nums = %s, target = %d, n = %d
            Output: %s
            """,
                Arrays.toString(nums), target, n, result
        );
    }
}
