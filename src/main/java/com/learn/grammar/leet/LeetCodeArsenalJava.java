package com.learn.grammar.leet;

import java.util.*;
import java.util.stream.Collectors;

public class LeetCodeArsenalJava {

    // === I. Data Structures ===

    // 1. Arrays
    public static class ArraysDemo {
        public static void run() {
            int[] arr = {1, 2, 3, 4, 5};
            int[] fixedSizeArr = new int[10]; // Initializes with 0s
            // Access: arr[0]
            // Length: arr.length
            // Iterate: for (int x : arr) { ... }
            // Sort: Arrays.sort(arr);
            // Fill: Arrays.fill(fixedSizeArr, -1);
            // Copy: int[] copy = Arrays.copyOf(arr, arr.length);
            //      int[] rangeCopy = Arrays.copyOfRange(arr, 1, 3); // [2, 3]

            // 2D Arrays
            int[][] matrix = new int[3][4];
            // matrix[row][col]

            // List to Array
            List<Integer> list = new ArrayList<>(List.of(1,2,3));
            Integer[] arrayFromList = list.toArray(new Integer[0]);
            int[] intArrayFromList = list.stream().mapToInt(i->i).toArray();

            // Array to List
            Integer[] anArray = {1,2,3};
            List<Integer> listFromArray = Arrays.asList(anArray); // Fixed-size list backed by array
            List<Integer> mutableListFromArray = new ArrayList<>(Arrays.asList(anArray));
        }
    }

    // 2. Strings
    public static class StringsDemo {
        public static void run() {
            String s = "hello"; // Immutable
            // Length: s.length()
            // Char at: s.charAt(0)
            // Substring: s.substring(1, 3) // "el" (end index is exclusive)
            // Concatenate: String s2 = s + " world";
            // Contains: s.contains("ell")
            // IndexOf: s.indexOf("lo")
            // Replace: s.replace('l', 'p')
            // Split: String[] parts = "a,b,c".split(",");
            // To char array: char[] chars = s.toCharArray();

            StringBuilder sb = new StringBuilder("mutable"); // Mutable
            sb.append(" string");
            sb.insert(0, "A ");
            sb.deleteCharAt(1);
            sb.reverse();
            String result = sb.toString();
            // System.out.println(result);
        }
    }

    // 3. Linked Lists
    public static class ListNode {
        public int val;
        public ListNode next;
        public ListNode(int val) { this.val = val; }
        public ListNode(int val, ListNode next) { this.val = val; this.next = next; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            ListNode curr = this;
            while (curr != null) {
                sb.append(curr.val).append(" -> ");
                curr = curr.next;
            }
            if (sb.length() > 0) sb.setLength(sb.length() - 4); // Remove last " -> "
            return sb.toString();
        }
    }
    // Example: ListNode head = new ListNode(1, new ListNode(2, new ListNode(3)));

    public static class DoublyListNode {
        public int val;
        public DoublyListNode prev;
        public DoublyListNode next;
        public DoublyListNode(int val) { this.val = val; }
    }


    // 4. Hash Tables (Map, Set)
    public static class HashTablesDemo {
        public static void run() {
            // HashMap
            Map<String, Integer> map = new HashMap<>();
            map.put("one", 1);
            map.put("two", 2);
            // Get: map.get("one")
            // Get with default: map.getOrDefault("three", 0)
            // Contains key: map.containsKey("one")
            // Remove: map.remove("one")
            // Size: map.size()
            // Iterate keys: for (String key : map.keySet()) { ... }
            // Iterate values: for (Integer value : map.values()) { ... }
            // Iterate entries: for (Map.Entry<String, Integer> entry : map.entrySet()) { ... }
            // map.forEach((k, v) -> System.out.println(k + ":" + v));

            // TreeMap (Sorted Map by keys)
            Map<String, Integer> treeMap = new TreeMap<>();
            treeMap.put("c", 3);
            treeMap.put("a", 1);
            treeMap.put("b", 2); // Keys will be iterated in "a", "b", "c" order

            // LinkedHashMap (Maintains insertion order)
            Map<String, Integer> linkedHashMap = new LinkedHashMap<>();
            linkedHashMap.put("c", 3);
            linkedHashMap.put("a", 1);
            linkedHashMap.put("b", 2); // Iteration order: "c", "a", "b"

            // HashSet
            Set<Integer> set = new HashSet<>();
            set.add(1);
            set.add(2);
            set.add(1); // Ignored, set contains {1, 2}
            // Contains: set.contains(1)
            // Remove: set.remove(1)
            // Size: set.size()
            // Iterate: for (int item : set) { ... }

            // TreeSet (Sorted Set)
            Set<Integer> treeSet = new TreeSet<>();
            treeSet.add(3); treeSet.add(1); treeSet.add(2); // {1, 2, 3}

            // LinkedHashSet (Maintains insertion order)
            Set<Integer> linkedHashSet = new LinkedHashSet<>();
            linkedHashSet.add(3); linkedHashSet.add(1); linkedHashSet.add(2); // Iteration order: 3, 1, 2

            // Frequency Counter
            Map<Character, Integer> counts = new HashMap<>();
            String text = "hello world";
            for (char c : text.toCharArray()) {
                counts.put(c, counts.getOrDefault(c, 0) + 1);
            }
        }
    }

    // 5. Stacks (LIFO)
    public static class StacksDemo {
        public static void run() {
            // Using Deque (recommended over java.util.Stack)
            Deque<Integer> stack = new ArrayDeque<>();
            stack.push(1); // or stack.addFirst(1);
            stack.push(2);
            // Pop: int top = stack.pop(); // or stack.removeFirst(); (2)
            // Peek: int peek = stack.peek(); // or stack.peekFirst(); (1)
            // isEmpty: stack.isEmpty()
        }
    }

    // 6. Queues (FIFO)
    public static class QueuesDemo {
        public static void run() {
            // Using Deque or LinkedList
            Queue<Integer> queue = new LinkedList<>(); // Or ArrayDeque for efficiency
            queue.offer(1); // or queue.add(1); (add throws exception if full for bounded queues)
            queue.offer(2);
            // Dequeue: int front = queue.poll(); // or queue.remove(); (remove throws exception if empty) (1)
            // Peek: int peek = queue.peek(); // (2)
            // isEmpty: queue.isEmpty()
        }
    }

    // 7. Heaps (Priority Queues)
    public static class HeapsDemo {
        public static void run() {
            // Min-Heap (default)
            PriorityQueue<Integer> minHeap = new PriorityQueue<>();
            minHeap.offer(3);
            minHeap.offer(1);
            minHeap.offer(4);
            // Smallest: minHeap.peek() (1)
            // Pop smallest: minHeap.poll() (1)

            // Max-Heap
            // Option 1: Collections.reverseOrder()
            PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
            // Option 2: Custom comparator (lambda)
            // PriorityQueue<Integer> maxHeap = new PriorityQueue<>((a, b) -> b - a);
            maxHeap.offer(3);
            maxHeap.offer(1);
            maxHeap.offer(4);
            // Largest: maxHeap.peek() (4)
            // Pop largest: maxHeap.poll() (4)

            // Heap with custom objects
            // class Point { int x, y, dist; /* constructor, getters */ }
            // PriorityQueue<Point> pointHeap = new PriorityQueue<>((p1, p2) -> p1.dist - p2.dist); // Min-heap by dist

            // Heapify an existing collection (O(N))
            List<Integer> data = new ArrayList<>(List.of(5, 2, 8, 1, 9));
            PriorityQueue<Integer> heapFromList = new PriorityQueue<>(data); // Min-heap
        }
    }

    // 8. Trees
    public static class TreeNode {
        public int val;
        public TreeNode left;
        public TreeNode right;
        public TreeNode(int val) { this.val = val; }
        public TreeNode(int val, TreeNode left, TreeNode right) {
            this.val = val; this.left = left; this.right = right;
        }
    }
    // Example:
    // TreeNode root = new TreeNode(1,
    //     new TreeNode(2, new TreeNode(4), new TreeNode(5)),
    //     new TreeNode(3)
    // );

    // Tries (Prefix Trees)
    public static class TrieNode {
        public Map<Character, TrieNode> children = new HashMap<>();
        public boolean isEndOfWord = false;
    }

    public static class Trie {
        private TrieNode root;
        public Trie() { root = new TrieNode(); }

        public void insert(String word) {
            TrieNode current = root;
            for (char ch : word.toCharArray()) {
                current = current.children.computeIfAbsent(ch, k -> new TrieNode());
            }
            current.isEndOfWord = true;
        }

        public boolean search(String word) {
            TrieNode current = root;
            for (char ch : word.toCharArray()) {
                TrieNode node = current.children.get(ch);
                if (node == null) return false;
                current = node;
            }
            return current.isEndOfWord;
        }

        public boolean startsWith(String prefix) {
            TrieNode current = root;
            for (char ch : prefix.toCharArray()) {
                TrieNode node = current.children.get(ch);
                if (node == null) return false;
                current = node;
            }
            return true;
        }
    }

    // 9. Graphs
    public static class GraphsDemo {
        // Adjacency List (Map of Node -> List of Neighbors)
        // Map<Integer, List<Integer>> adjList = new HashMap<>();
        // void addEdge(int u, int v, boolean isDirected) {
        //     adjList.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
        //     if (!isDirected) {
        //         adjList.computeIfAbsent(v, k -> new ArrayList<>()).add(u);
        //     }
        // }

        // Adjacency List (List of Lists for nodes 0 to N-1)
        // int numNodes = 5;
        // List<List<Integer>> adjListInts = new ArrayList<>(numNodes);
        // for (int i = 0; i < numNodes; i++) adjListInts.add(new ArrayList<>());
        // void addEdgeInt(int u, int v, boolean isDirected) {
        //     adjListInts.get(u).add(v);
        //     if (!isDirected) adjListInts.get(v).add(u);
        // }

        // Adjacency Matrix
        // int[][] adjMatrix = new int[numNodes][numNodes]; // 1 for edge, 0 otherwise (or weight)
    }


    // === II. Algorithmic Paradigms ===

    // 1. Sorting
    public static class SortingDemo {
        public static void run() {
            int[] arr = {3, 1, 4, 1, 5, 9, 2, 6};
            Arrays.sort(arr); // In-place for primitives

            List<Integer> list = new ArrayList<>(List.of(3, 1, 4, 1, 5, 9, 2, 6));
            Collections.sort(list); // In-place for List of Comparable

            list.sort( (x, y) -> x - y);

            // Custom sort with Comparator (e.g., Points by y-coordinate)
            // class Point { int x, y; /* constructor */ }
            // List<Point> points = new ArrayList<>();
            // points.add(new Point(1,2)); points.add(new Point(3,1));
            // points.sort(Comparator.comparingInt(p -> p.y));
            // Or descending: points.sort((p1, p2) -> p2.y - p1.y);
        }
    }

    // 2. Searching
    public static class SearchingDemo {
        // Binary Search (requires sorted array)
        public static int binarySearch(int[] sortedArr, int target) {
            int left = 0, right = sortedArr.length - 1;
            while (left <= right) {
                int mid = left + (right - left) / 2; // Avoid overflow
                if (sortedArr[mid] == target) return mid;
                if (sortedArr[mid] < target) left = mid + 1;
                else right = mid - 1;
            }
            return -1; // Not found
        }
        // Java's built-in: Arrays.binarySearch(sortedArr, target);
        // Returns index if found, or (-(insertion point) - 1) if not found.
    }

    // 3. Recursion
    public static class RecursionDemo {
        public static long factorial(int n) {
            if (n < 0) throw new IllegalArgumentException("Negative input");
            if (n == 0 || n == 1) return 1;
            return n * factorial(n - 1);
        }
    }

    // 4. Backtracking
    public static class BacktrackingDemo {
        // Example: Permutations of a list
        public List<List<Integer>> permute(int[] nums) {
            List<List<Integer>> result = new ArrayList<>();
            backtrackPermute(result, new ArrayList<>(), nums, new boolean[nums.length]);
            return result;
        }

        private void backtrackPermute(List<List<Integer>> result, List<Integer> currentPermutation, int[] nums, boolean[] used) {
            if (currentPermutation.size() == nums.length) {
                result.add(new ArrayList<>(currentPermutation));
                return;
            }
            for (int i = 0; i < nums.length; i++) {
                if (used[i]) continue;

                used[i] = true;
                currentPermutation.add(nums[i]); // Choose

                backtrackPermute(result, currentPermutation, nums, used); // Explore

                currentPermutation.remove(currentPermutation.size() - 1); // Unchoose
                used[i] = false;
            }
        }
    }

    // 5. Dynamic Programming (DP)
    public static class DPDemo {
        // a) Memoization (Top-Down Fibonacci)
        public long fibMemo(int n, Map<Integer, Long> memo) {
            if (memo.containsKey(n)) return memo.get(n);
            if (n <= 0) return 0;
            if (n == 1) return 1;
            long result = fibMemo(n - 1, memo) + fibMemo(n - 2, memo);
            memo.put(n, result);
            return result;
        }
        // Call with: fibMemo(n, new HashMap<>());

        // b) Tabulation (Bottom-Up Fibonacci)
        public long fibTabulation(int n) {
            if (n <= 0) return 0;
            if (n == 1) return 1;
            long[] dp = new long[n + 1];
            dp[0] = 0;
            dp[1] = 1;
            for (int i = 2; i <= n; i++) {
                dp[i] = dp[i - 1] + dp[i - 2];
            }
            return dp[n];
        }
    }

    // 6. Greedy Algorithms
    // (Conceptual: make locally optimal choices. Example: Interval Scheduling, Huffman Coding)
    // Often problem-specific, requires proof of correctness.

    // 7. Graph Traversal Algorithms
    public static class GraphTraversalDemo {
        // BFS (using Queue) - for adjList: Map<Integer, List<Integer>> graph
        public List<Integer> bfs(Map<Integer, List<Integer>> graph, int startNode) {
            List<Integer> traversalOrder = new ArrayList<>();
            Queue<Integer> queue = new LinkedList<>();
            Set<Integer> visited = new HashSet<>();

            if (!graph.containsKey(startNode)) return traversalOrder;

            queue.offer(startNode);
            visited.add(startNode);

            while (!queue.isEmpty()) {
                int currentNode = queue.poll();
                traversalOrder.add(currentNode);

                for (int neighbor : graph.getOrDefault(currentNode, Collections.emptyList())) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.offer(neighbor);
                    }
                }
            }
            return traversalOrder;
        }

        // DFS (Recursive) - for adjList: Map<Integer, List<Integer>> graph
        public List<Integer> dfsRecursive(Map<Integer, List<Integer>> graph, int startNode) {
            List<Integer> traversalOrder = new ArrayList<>();
            Set<Integer> visited = new HashSet<>();
            dfsHelper(graph, startNode, visited, traversalOrder);
            return traversalOrder;
        }
        private void dfsHelper(Map<Integer, List<Integer>> graph, int currentNode, Set<Integer> visited, List<Integer> path) {
            if (!graph.containsKey(currentNode) || visited.contains(currentNode)) return;

            visited.add(currentNode);
            path.add(currentNode);

            for (int neighbor : graph.getOrDefault(currentNode, Collections.emptyList())) {
                if (!visited.contains(neighbor)) {
                    dfsHelper(graph, neighbor, visited, path);
                }
            }
        }

        // Tree Traversals (for TreeNode class defined earlier)
        public List<Integer> inorderTraversal(TreeNode root) {
            List<Integer> result = new ArrayList<>();
            inorderHelper(root, result);
            return result;
        }
        private void inorderHelper(TreeNode node, List<Integer> res) { // LNR
            if (node == null) return;
            inorderHelper(node.left, res);
            res.add(node.val);
            inorderHelper(node.right, res);
        }
        // Preorder (NLR) and Postorder (LRN) are similar, just change order of operations.
    }

    // 8. Two Pointers
    public static class TwoPointersDemo {
        // Example: Check if string is palindrome (ignoring non-alphanumeric & case)
        public boolean isPalindrome(String s) {
            int left = 0, right = s.length() - 1;
            while (left < right) {
                while (left < right && !Character.isLetterOrDigit(s.charAt(left))) left++;
                while (left < right && !Character.isLetterOrDigit(s.charAt(right))) right--;
                if (Character.toLowerCase(s.charAt(left)) != Character.toLowerCase(s.charAt(right))) {
                    return false;
                }
                left++;
                right--;
            }
            return true;
        }
    }

    // 9. Sliding Window
    public static class SlidingWindowDemo {
        // Max sum of a subarray of size k
        public int maxSumSubarrayOfSizeK(int[] arr, int k) {
            if (arr == null || arr.length < k || k <= 0) return 0; // Or throw exception
            int currentSum = 0;
            for (int i = 0; i < k; i++) {
                currentSum += arr[i];
            }
            int maxSum = currentSum;
            for (int i = k; i < arr.length; i++) {
                currentSum += arr[i] - arr[i - k]; // Slide window
                maxSum = Math.max(maxSum, currentSum);
            }
            return maxSum;
        }
    }

    // 10. Divide and Conquer
    public static class DivideAndConquerDemo {
        // Merge Sort
        public void mergeSort(int[] arr) {
            if (arr == null || arr.length <= 1) return;
            int[] temp = new int[arr.length]; // Helper array for merging
            mergeSortHelper(arr, temp, 0, arr.length - 1);
        }
        private void mergeSortHelper(int[] arr, int[] temp, int left, int right) {
            if (left >= right) return;
            int mid = left + (right - left) / 2;
            mergeSortHelper(arr, temp, left, mid);
            mergeSortHelper(arr, temp, mid + 1, right);
            merge(arr, temp, left, mid, right);
        }
        private void merge(int[] arr, int[] temp, int left, int mid, int right) {
            System.arraycopy(arr, left, temp, left, right - left + 1);
            int i = left, j = mid + 1, k = left;
            while (i <= mid && j <= right) {
                arr[k++] = (temp[i] <= temp[j]) ? temp[i++] : temp[j++];
            }
            while (i <= mid) arr[k++] = temp[i++];
            // No need to copy right part if already in place from arr itself
        }
    }

    // 11. Bit Manipulation
    public static class BitManipulationDemo {
        public static void run() {
            int num = 13; // Binary: 1101
            // Get bit at position i (0-indexed from right): (number >> i) & 1
            // System.out.println("Bit at pos 0: " + ((num >> 0) & 1)); // 1
            // Set bit at position i: number | (1 << i)
            // System.out.println("Set bit at pos 1: " + Integer.toBinaryString(num | (1 << 1))); // 1111 (15)
            // Clear bit at position i: number & ~(1 << i)
            // System.out.println("Clear bit at pos 0: " + Integer.toBinaryString(num & ~(1 << 0))); // 1100 (12)
            // Toggle bit at position i: number ^ (1 << i)
            // System.out.println("Toggle bit at pos 2: " + Integer.toBinaryString(num ^ (1 << 2))); // 1001 (9)
            // Check if power of 2: (n > 0) && ((n & (n - 1)) == 0)
            // Count set bits (Hamming weight): Integer.bitCount(n)
            // System.out.println("Count set bits in " + num + ": " + Integer.bitCount(num)); // 3
        }
    }

    public static void main(String[] args) {
        System.out.println("Java LeetCode Arsenal Ready!");
        // Example usage:
        // ArraysDemo.run();
        // StringsDemo.run();
        // HashTablesDemo.run();
        // StacksDemo.run();
        // QueuesDemo.run();
        // HeapsDemo.run();

        // ListNode head = new ListNode(1, new ListNode(2, new ListNode(3)));
        // System.out.println("Linked List: " + head);

        // Trie trie = new Trie();
        // trie.insert("apple");
        // System.out.println("Trie search 'apple': " + trie.search("apple"));
        // System.out.println("Trie startsWith 'app': " + trie.startsWith("app"));

        // SortingDemo.run();
        // System.out.println("Binary search for 23: " + SearchingDemo.binarySearch(new int[]{1,2,5,8,12,16,23,38,56,72,91}, 23));
        // System.out.println("Factorial of 5: " + RecursionDemo.factorial(5));

        // BacktrackingDemo btd = new BacktrackingDemo();
        // System.out.println("Permutations of [1,2,3]: " + btd.permute(new int[]{1,2,3}));

        // DPDemo dp = new DPDemo();
        // System.out.println("Fib Memo(10): " + dp.fibMemo(10, new HashMap<>()));
        // System.out.println("Fib Tab(10): " + dp.fibTabulation(10));

        // Graph Example (BFS/DFS)
        // Map<Integer, List<Integer>> adjList = new HashMap<>();
        // adjList.computeIfAbsent(0, k -> new ArrayList<>()).addAll(List.of(1, 2));
        // adjList.computeIfAbsent(1, k -> new ArrayList<>()).add(2);
        // adjList.computeIfAbsent(2, k -> new ArrayList<>()).addAll(List.of(0, 3));
        // adjList.computeIfAbsent(3, k -> new ArrayList<>()).add(3);
        // GraphTraversalDemo gtd = new GraphTraversalDemo();
        // System.out.println("BFS from 2: " + gtd.bfs(adjList, 2));
        // System.out.println("DFS from 2: " + gtd.dfsRecursive(adjList, 2));

        // TwoPointersDemo tpd = new TwoPointersDemo();
        // System.out.println("Is 'A man, a plan, a canal: Panama' palindrome? " + tpd.isPalindrome("A man, a plan, a canal: Panama"));

        // SlidingWindowDemo swd = new SlidingWindowDemo();
        // System.out.println("Max sum subarray size 3 of [2,1,5,1,3,2]: " + swd.maxSumSubarrayOfSizeK(new int[]{2,1,5,1,3,2}, 3));

        // DivideAndConquerDemo dcd = new DivideAndConquerDemo();
        // int[] arrToSort = {3, 1, 4, 1, 5, 9, 2, 6};
        // dcd.mergeSort(arrToSort);
        // System.out.println("Merge sorted array: " + Arrays.toString(arrToSort));

        // BitManipulationDemo.run();
    }
}
