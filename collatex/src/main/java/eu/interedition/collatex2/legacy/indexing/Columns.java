package eu.interedition.collatex2.legacy.indexing;

import java.util.List;

import com.google.common.collect.Lists;

import eu.interedition.collatex2.interfaces.IColumn;
import eu.interedition.collatex2.interfaces.INormalizedToken;
import eu.interedition.collatex2.interfaces.IPhrase;
import eu.interedition.collatex2.interfaces.nonpublic.modifications.IColumns;

public class Columns implements IColumns {
  private final List<IColumn> columns;

  public Columns(final List<IColumn> columns1) {
    this.columns = columns1;
  }

  public Columns() {
    this.columns = Lists.newArrayList();
  }

  //NOTE: for now we assume that phraseA is longer than phraseB!
  //NOTE: this method is only for matches!
  @Override
  public void addMatchPhrase(final IPhrase phraseB) {
    if (phraseB.size() > columns.size()) {
      // System.out.println(columns.size());
      // System.out.println(phraseB.size());
      throw new RuntimeException("The phrase to be placed in the table is longer than columns!");
    }
    final List<INormalizedToken> tokens = phraseB.getTokens();
    for (int i = 0; i < phraseB.size(); i++) {
      final IColumn column = columns.get(i);
      final INormalizedToken token = tokens.get(i);
      // NOTE: REMARKED because of the use of legacy API!
      //      column.addMatch(token);
    }
  }

  //NOTE: for now we assume that phraseA is longer than phraseB!
  //NOTE: this method is only for variants!
  @Override
  public void addVariantPhrase(final IPhrase phraseB) {
    if (phraseB.size() > columns.size()) {
      // System.out.println("!!" + toString() + ":" + phraseB.toString());
      // System.out.println(columns.size());
      // System.out.println(phraseB.size());
      throw new RuntimeException("The phrase to be placed in the table is longer than columns!");
    }
    final List<INormalizedToken> tokens = phraseB.getTokens();
    for (int i = 0; i < phraseB.size(); i++) {
      final IColumn column = columns.get(i);
      final INormalizedToken token = tokens.get(i);
      // NOTE: REMARKED because of the use of legacy API!
      //      column.addVariant(token);
    }
  }

  @Override
  public int getBeginPosition() {
    throw new RuntimeException("NOT IMPLEMENTED ANYMORE!");
  }

  @Override
  public int getEndPosition() {
    throw new RuntimeException("NOT IMPLEMENTED ANYMORE!");
  }

  @Override
  public IColumn getFirstColumn() {
    if (isEmpty()) {
      throw new RuntimeException("Columns are empty!");
    }
    return columns.get(0);
  }

  @Override
  public IColumn getLastColumn() {
    if (isEmpty()) {
      throw new RuntimeException("Columns are empty!");
    }
    return columns.get(columns.size() - 1);
  }

  @Override
  public boolean isEmpty() {
    return columns.isEmpty();
  }

  @Override
  public String toString() {
    StringBuffer buffer = new StringBuffer();
    String splitter="";
    for (IColumn column : columns) {
      buffer.append(splitter);
      buffer.append(column.toString());
      splitter = " ";
    }
    return buffer.toString();
  }
//    if (isEmpty()) {
//      return "Columns: EMPTY";
//    }
//    return "Columns: " + getBeginPosition() + "-" + getEndPosition();
//  }

  @Override
  public int size() {
    return columns.size();
  }

  @Override
  public List<IColumn> getColumns() {
    return columns;
  }

}
