package com.learn.grammar.effective;

import java.util.*;

public class ConsistentHashingTest {
    private final TreeMap<Long, String> virtualNodes = new TreeMap<>();
    private final int virtualNodeCount;
    private final List<String> realNodes = new ArrayList<>();

    public ConsistentHashingTest(int virtualNodeCount, List<String> realNodes) {
        this.virtualNodeCount = virtualNodeCount;
        this.realNodes.addAll(realNodes);

        for (String node : realNodes) {
            addNode(node);
        }
    }

    public void addNode(String node) {
        for (int i = 0; i < virtualNodeCount; i++) {
            String virtualNodeName = node + '#' + i;
            long hash = getHash(virtualNodeName);
            virtualNodes.put(hash, node);
        }
    }

    public void removeNode(String node) {
        for (int i = 0; i < virtualNodeCount; i++) {
            String virtualNodeName = node + '#' + i;
            long hash = getHash(virtualNodeName);
            virtualNodes.remove(hash);
        }
    }

    public String getNode(String key) {
        if(virtualNodes.isEmpty()) {
            return null;
        }

        long hash = getHash(key);

        Map.Entry<Long, String> entry = virtualNodes.ceilingEntry(hash);
        if(entry == null){
            entry = virtualNodes.firstEntry();
        }

        return entry.getValue();
    }

    // FNV1_32_HASH
    private long getHash(String key) {
        final int p = 16777619;
        long hash = 2166136261L;

        for (int i = 0; i < key.length(); i++) {
            hash = (hash ^ key.charAt(i)) * p;
        }
        hash += hash << 13;
        hash ^= hash >> 7;
        hash += hash << 3;
        hash ^= hash >> 17;
        hash += hash << 5;
        return Math.abs(hash);
    }
    public static void main(String[] args) {
        // 三个真实节点
        List<String> nodes = Arrays.asList(
                "192.168.0.1",
                "192.168.0.2",
                "192.168.0.3"
        );

        // 每个真实节点对应 100 个虚拟节点
        ConsistentHashingTest ch = new ConsistentHashingTest(100, nodes);

        // 测试 key 的分布
        Map<String, Integer> nodeCount = new HashMap<>();
        for (int i = 0; i < 10000; i++) {
            String key = "request_" + i;
            String node = ch.getNode(key);
            nodeCount.put(node, nodeCount.getOrDefault(node, 0) + 1);
        }

        // 输出每个节点的请求数量
        System.out.println("请求分布情况:");
        nodeCount.forEach((node, count) -> {
            System.out.println(node + ": " + count + " 次");
        });

        // 移除一个节点，观察影响
        System.out.println("\n移除 192.168.0.2 后:");
        ch.removeNode("192.168.0.2");
        nodeCount.clear();
        for (int i = 0; i < 10000; i++) {
            String key = "request_" + i;
            String node = ch.getNode(key);
            nodeCount.put(node, nodeCount.getOrDefault(node, 0) + 1);
        }
        nodeCount.forEach((node, count) -> {
            System.out.println(node + ": " + count + " 次");
        });
    }
}
