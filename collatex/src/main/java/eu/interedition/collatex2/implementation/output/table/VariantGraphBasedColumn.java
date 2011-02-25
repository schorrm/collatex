package eu.interedition.collatex2.implementation.output.table;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.common.collect.Lists;

import eu.interedition.collatex2.interfaces.ColumnState;
import eu.interedition.collatex2.interfaces.IColumn;
import eu.interedition.collatex2.interfaces.INormalizedToken;
import eu.interedition.collatex2.interfaces.IVariantGraphVertex;
import eu.interedition.collatex2.interfaces.IWitness;

public class VariantGraphBasedColumn implements IColumn {
  private final List<IVariantGraphVertex> vertices;

  public VariantGraphBasedColumn(IVariantGraphVertex vertex) {
    this.vertices = Lists.newArrayList();
    addVertex(vertex);
  }

  @Override
  public ColumnState getState() {
    if (vertices.size() == 1) {
      return ColumnState.INVARIANT;
    }
    return ColumnState.VARIANT;
  }

  @Override
  public INormalizedToken getToken(IWitness witness) {
    IVariantGraphVertex vertex = findVertexForWitness(witness);
    if (vertex == null) {
      throw new NoSuchElementException("Witness " + witness.getSigil() + " is not present in this column");
    }
    return vertex.getToken(witness);
  }

  //TODO: add/re-enable test (see parallel segmentation tests)
  @Override
  public List<IWitness> getWitnesses() {
    List<IWitness> totalWitnesses = Lists.newArrayList();
    for (IVariantGraphVertex vertex : vertices) {
      Set<IWitness> witnesses = vertex.getWitnesses();
      totalWitnesses.addAll(witnesses);
    }
    return totalWitnesses;
  }

  //TODO: make non public!
  @Override
  public void addVertex(IVariantGraphVertex vertex) {
    vertices.add(vertex);
  }

  @Override
  public boolean containsWitness(IWitness witness) {
    IVariantGraphVertex findVertexForWitness = findVertexForWitness(witness);
    return findVertexForWitness != null;
  }


  // should maybe be a map?
  protected IVariantGraphVertex findVertexForWitness(IWitness witness) {
    for (IVariantGraphVertex vertex : vertices) {
      if (vertex.containsWitness(witness)) {
        return vertex;
      }
    }
    return null;
  }

}
