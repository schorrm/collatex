package com.sd_editions.collatex.Collate;

import java.util.ArrayList;

import com.google.common.collect.Lists;
import com.sd_editions.collatex.Block.Block;
import com.sd_editions.collatex.Block.BlockStructure;
import com.sd_editions.collatex.Block.IntBlockVisitor;
import com.sd_editions.collatex.Block.Line;
import com.sd_editions.collatex.Block.Word;

public class WordAlignmentVisitor implements IntBlockVisitor {
  private Block witnessBlock;
  private ArrayList<Tuple> result;
  private int baseIndex = 1;
  private int witnessIndex = 1;
  private int countDistance = 3;

  public WordAlignmentVisitor(BlockStructure variant) {
    this.witnessBlock = variant.getRootBlock();
  }

  public WordAlignmentVisitor(Word variant) {
    this.witnessBlock = variant;
    this.result = Lists.newArrayList();
    createResult();
  }

  private void createResult() {
    result = Lists.newArrayList();
  }

  public void visitBlockStructure(BlockStructure blockStructure) {
    Block rootBlock = blockStructure.getRootBlock();
    rootBlock.accept(this);
  }

  public void visitLine(Line line) {
    createResult();
    this.witnessBlock = witnessBlock.getFirstChild();
    Word w = (Word) line.getFirstChild();
    w.accept(this);
    while (w.hasNextSibling()) {
      w = (Word) w.getNextSibling();
      baseIndex++;
      w.accept(this);
    }
    // TODO move to next line etc...
  }

  public boolean lookForBetterMatch(Word baseWord, Word witnessWord) {
    int count = this.countDistance;
    Word witnessWordOld = witnessWord;
    int witnessIndexOld = witnessIndex;
    Block witnessBlockOld = witnessBlock;
    while (witnessWord.hasNextSibling() && count > 0) {
      witnessWord = (Word) witnessWord.getNextSibling();
      witnessIndex++;
      witnessBlock = witnessBlock.getNextSibling();
      count--;
      if (baseWord.alignmentFactor(witnessWord) == 0) {
        result.add(new Tuple(baseIndex, witnessIndex));
        return true;
      }
    }
    witnessIndex = witnessIndexOld;
    witnessWord = witnessWordOld;
    witnessBlock = witnessBlockOld;
    return false;
  }

  public void visitWord(Word baseWord) {
    Word witnessWord = (Word) witnessBlock;
    //    System.out.println("visitWord: comparing " + baseWord + " + " + witnessWord);
    //    System.out.println("index: [" + baseIndex + "," + witnessIndex + "]");
    if (witnessWord == null) {
      return;
    }

    if (baseWord.alignsWith(witnessWord)) {
      System.out.println(baseWord + "-1-" + witnessWord);
      //look, if there're a better match maybe
      boolean foundBetterMatch = false;
      if (baseWord.alignmentFactor(witnessWord) == 1) {
        foundBetterMatch = lookForBetterMatch(baseWord, witnessWord);
      }
      if (!foundBetterMatch) {
        result.add(new Tuple(baseIndex, witnessIndex));
        witnessIndex++;
        witnessBlock = witnessBlock.getNextSibling();
      }

    } else {
      // now, first try to find a match in the witnessBlock for the baseWord
      Block savedSegmentPosition = witnessBlock;
      int savedWitnessIndex = witnessIndex;
      boolean foundMatchInWitness = false;
      while (!foundMatchInWitness && witnessWord.hasNextSibling()) {
        witnessWord = (Word) witnessWord.getNextSibling();
        witnessIndex++;
        witnessBlock = witnessBlock.getNextSibling();
        if (baseWord.alignsWith(witnessWord)) {
          //look, if there're a better match
          System.out.println(baseWord + "-2-" + witnessWord);
          boolean foundBetterMatch = false;
          if (baseWord.alignmentFactor(witnessWord) == 1) {
            foundBetterMatch = lookForBetterMatch(baseWord, witnessWord);
          }
          if (!foundBetterMatch) {
            result.add(new Tuple(baseIndex, witnessIndex));
            witnessIndex++;
            witnessBlock = witnessBlock.getNextSibling();
          }
          foundMatchInWitness = true;
        }
      }
      if (!foundMatchInWitness) {
        // reset
        witnessBlock = savedSegmentPosition;
        witnessWord = (Word) savedSegmentPosition;
        witnessIndex = savedWitnessIndex;
      }
    }
  }

  public Tuple[] getResult() {
    //    System.out.println("getResult: " + result);
    return result.toArray(new Tuple[] {});
  }

  public void setCountDistance(int countDistance) {
    this.countDistance = countDistance;
  }

}