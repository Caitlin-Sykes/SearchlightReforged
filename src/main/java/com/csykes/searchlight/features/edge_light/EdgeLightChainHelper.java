package com.csykes.searchlight.features.edge_light;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Discovers and builds an ordered sequence of addressable pixel targets
 * along a contiguous chain of {@link EdgeLightBlock}s.
 */
public class EdgeLightChainHelper {

    public record PixelTarget(@NotNull BlockPos pos, @NotNull Direction edge, int subPixelIndex) {
        public PixelTarget(@NotNull BlockPos pos, @NotNull Direction edge) {
            this(pos, edge, 0);
        }
    }

    public record EdgeNode(@NotNull BlockPos pos, @NotNull Direction edge) {
        @Override
        public @NotNull String toString() {
            return pos.toShortString() + ":" + edge.getName();
        }
    }

    private static final int MAX_CHAIN_LENGTH = 256;

    /**
     * Builds an ordered list of PixelTargets for a connected edge light strip starting
     * from the given anchor block position.
     */
    public static List<PixelTarget> getChain(@NotNull Level level, @NotNull BlockPos startPos) {
        BlockState startState = level.getBlockState(startPos);
        if (!(startState.getBlock() instanceof EdgeLightBlock)) {
            return Collections.emptyList();
        }

        // Find all connected (BlockPos, Edge) nodes
        Map<EdgeNode, List<EdgeNode>> adjacency = new LinkedHashMap<>();
        Set<EdgeNode> visited = new HashSet<>();
        Queue<EdgeNode> queue = new ArrayDeque<>();

        // Seed with all active edges on the start block
        for (Direction dir : EdgeLightData.HORIZONTALS) {
            BooleanProperty prop = EdgeLightData.getPropertyForDirection(dir);
            if (prop != null && startState.hasProperty(prop) && startState.getValue(prop)) {
                EdgeNode node = new EdgeNode(startPos, dir);
                visited.add(node);
                queue.add(node);
            }
        }

        // BFS to find all connected edges
        while (!queue.isEmpty() && adjacency.size() < MAX_CHAIN_LENGTH) {
            EdgeNode current = queue.poll();
            List<EdgeNode> neighbors = getConnectedNeighbors(level, current);
            adjacency.put(current, neighbors);

            for (EdgeNode neighbor : neighbors) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        if (adjacency.isEmpty()) {
            return Collections.emptyList();
        }

        // Order the nodes into a continuous linear path or loop
        List<EdgeNode> orderedPath = orderNodes(adjacency, startPos);

        return buildSubPixelTargets(orderedPath);
    }

    private static List<PixelTarget> buildSubPixelTargets(List<EdgeNode> orderedPath) {
        List<PixelTarget> targets = new ArrayList<>();
        if (orderedPath.isEmpty()) return targets;

        int n = orderedPath.size();
        if (n == 1) {
            EdgeNode node = orderedPath.get(0);
            for (int k = 0; k < EdgeLightData.SUB_PIXELS_PER_EDGE; k++) {
                targets.add(new PixelTarget(node.pos(), node.edge(), k));
            }
            return targets;
        }

        Vertex currentExit = null;
        Vertex firstEntry = null;

        for (int i = 0; i < n; i++) {
            EdgeNode node = orderedPath.get(i);
            Vertex[] ep = getEndpoints(node.pos(), node.edge());
            if (ep.length < 2) continue;

            boolean forward;
            if (i == 0) {
                EdgeNode nextNode = orderedPath.get(1);
                Vertex[] nextEp = getEndpoints(nextNode.pos(), nextNode.edge());
                if (nextEp.length >= 2 && (ep[1].equals(nextEp[0]) || ep[1].equals(nextEp[1]))) {
                    forward = true;
                } else if (nextEp.length >= 2 && (ep[0].equals(nextEp[0]) || ep[0].equals(nextEp[1]))) {
                    forward = false;
                } else {
                    forward = true;
                }
                firstEntry = forward ? ep[0] : ep[1];
                currentExit = forward ? ep[1] : ep[0];
            } else {
                if (ep[0].equals(currentExit)) {
                    forward = true;
                    currentExit = ep[1];
                } else if (ep[1].equals(currentExit)) {
                    forward = false;
                    currentExit = ep[0];
                } else {
                    if (i + 1 < n) {
                        Vertex[] nextEp = getEndpoints(orderedPath.get(i + 1).pos(), orderedPath.get(i + 1).edge());
                        if (nextEp.length >= 2 && (ep[1].equals(nextEp[0]) || ep[1].equals(nextEp[1]))) {
                            forward = true;
                        } else if (nextEp.length >= 2 && (ep[0].equals(nextEp[0]) || ep[0].equals(nextEp[1]))) {
                            forward = false;
                        } else {
                            forward = true;
                        }
                    } else {
                        forward = true;
                    }
                    currentExit = forward ? ep[1] : ep[0];
                }
            }

            // Skip corner on entering edge if transitioning between perpendicular edges on the same block
            boolean skipEntry = (i > 0) && node.pos().equals(orderedPath.get(i - 1).pos());

            // Skip closing corner on last edge if looping back to start on the same block
            boolean skipExit = (i == n - 1) && (n > 2)
                    && currentExit != null && currentExit.equals(firstEntry)
                    && node.pos().equals(orderedPath.get(0).pos());

            int startK = forward ? (skipEntry ? 1 : 0) : (skipEntry ? 6 : 7);
            int endK = forward ? (skipExit ? 6 : 7) : (skipExit ? 1 : 0);
            int step = forward ? 1 : -1;

            for (int k = startK; forward ? (k <= endK) : (k >= endK); k += step) {
                targets.add(new PixelTarget(node.pos(), node.edge(), k));
            }
        }

        return targets;
    }

    private record Vertex(int x, int z) {}

    private static Vertex[] getEndpoints(BlockPos pos, Direction edge) {
        int x = pos.getX();
        int z = pos.getZ();
        return switch (edge) {
            case NORTH -> new Vertex[]{new Vertex(x, z), new Vertex(x + 1, z)};
            case SOUTH -> new Vertex[]{new Vertex(x, z + 1), new Vertex(x + 1, z + 1)};
            case WEST  -> new Vertex[]{new Vertex(x, z), new Vertex(x, z + 1)};
            case EAST  -> new Vertex[]{new Vertex(x + 1, z), new Vertex(x + 1, z + 1)};
            default -> new Vertex[0];
        };
    }

    private static List<EdgeNode> getCandidateEdgesAtVertex(Vertex v, int y) {
        int vx = v.x();
        int vz = v.z();
        return List.of(
                new EdgeNode(new BlockPos(vx, y, vz), Direction.NORTH),
                new EdgeNode(new BlockPos(vx, y, vz), Direction.WEST),
                new EdgeNode(new BlockPos(vx - 1, y, vz), Direction.NORTH),
                new EdgeNode(new BlockPos(vx - 1, y, vz), Direction.EAST),
                new EdgeNode(new BlockPos(vx, y, vz - 1), Direction.SOUTH),
                new EdgeNode(new BlockPos(vx, y, vz - 1), Direction.WEST),
                new EdgeNode(new BlockPos(vx - 1, y, vz - 1), Direction.SOUTH),
                new EdgeNode(new BlockPos(vx - 1, y, vz - 1), Direction.EAST)
        );
    }

    private static boolean sharesBothEndpoints(Vertex[] a, Vertex[] b) {
        return (a[0].equals(b[0]) && a[1].equals(b[1])) || (a[0].equals(b[1]) && a[1].equals(b[0]));
    }

    /**
     * Finds adjacent EdgeNodes connected to the given node.
     * Supports collinear runs, internal corners, and external corners around obstacles.
     */
    private static List<EdgeNode> getConnectedNeighbors(Level level, EdgeNode node) {
        List<EdgeNode> neighbors = new ArrayList<>();
        BlockPos pos = node.pos();
        Direction edge = node.edge();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof EdgeLightBlock)) return neighbors;

        AttachFace face = state.hasProperty(EdgeLightBlock.FACE) ? state.getValue(EdgeLightBlock.FACE) : AttachFace.FLOOR;
        Vertex[] endpoints = getEndpoints(pos, edge);
        if (endpoints.length < 2) return neighbors;

        Set<EdgeNode> added = new HashSet<>();

        for (Vertex endpoint : endpoints) {
            List<EdgeNode> candidates = getCandidateEdgesAtVertex(endpoint, pos.getY());
            for (EdgeNode candidate : candidates) {
                if (candidate.equals(node) || added.contains(candidate)) {
                    continue;
                }

                Vertex[] cEndpoints = getEndpoints(candidate.pos(), candidate.edge());
                if (cEndpoints.length == 2 && sharesBothEndpoints(endpoints, cEndpoints)) {
                    continue;
                }

                BlockPos cPos = candidate.pos();
                if (!level.isLoaded(cPos)) continue;

                BlockState cState = level.getBlockState(cPos);
                if (!(cState.getBlock() instanceof EdgeLightBlock)) continue;

                AttachFace cFace = cState.hasProperty(EdgeLightBlock.FACE) ? cState.getValue(EdgeLightBlock.FACE) : AttachFace.FLOOR;
                if (cFace != face) continue;

                BooleanProperty prop = EdgeLightData.getPropertyForDirection(candidate.edge());
                if (prop != null && cState.hasProperty(prop) && cState.getValue(prop)) {
                    neighbors.add(candidate);
                    added.add(candidate);
                }
            }
        }

        return neighbors;
    }

    /**
     * Orders the discovered adjacency graph into a linear/loop sequence.
     * Prefers starting at an endpoint (degree == 1) if one exists.
     */
    private static List<EdgeNode> orderNodes(Map<EdgeNode, List<EdgeNode>> adjacency, BlockPos anchorPos) {
        List<EdgeNode> result = new ArrayList<>();
        if (adjacency.isEmpty()) return result;

        // Find endpoint (degree == 1) to start linear strip traversal
        EdgeNode startNode = null;
        for (Map.Entry<EdgeNode, List<EdgeNode>> entry : adjacency.entrySet()) {
            if (entry.getValue().size() == 1) {
                startNode = entry.getKey();
                break;
            }
        }

        // If no degree-1 endpoint (e.g. closed loop), start from an edge on the anchor pos
        if (startNode == null) {
            for (EdgeNode node : adjacency.keySet()) {
                if (node.pos().equals(anchorPos)) {
                    startNode = node;
                    break;
                }
            }
            if (startNode == null) {
                startNode = adjacency.keySet().iterator().next();
            }
        }

        Set<EdgeNode> visited = new HashSet<>();
        EdgeNode current = startNode;

        while (current != null && visited.size() < adjacency.size()) {
            visited.add(current);
            result.add(current);

            List<EdgeNode> neighbors = adjacency.getOrDefault(current, Collections.emptyList());
            EdgeNode next = null;
            for (EdgeNode neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    next = neighbor;
                    break;
                }
            }
            current = next;
        }

        // Add any disconnected/remaining nodes if graph had multiple branches
        for (EdgeNode node : adjacency.keySet()) {
            if (!visited.contains(node)) {
                result.add(node);
                visited.add(node);
            }
        }

        return result;
    }
}
