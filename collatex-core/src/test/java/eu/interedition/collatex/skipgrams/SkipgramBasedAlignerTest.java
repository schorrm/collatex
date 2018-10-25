package eu.interedition.collatex.skipgrams;

import eu.interedition.collatex.simple.SimpleWitness;
import org.junit.Test;

/*
 * 23-10-2018
 *
 * Boring unit tests to do the boring stuff..
 *
 */
public class SkipgramBasedAlignerTest {

    @Test
    public void testAligner() {
        SimpleWitness w1 = new SimpleWitness("w1", "a b c d e");
        SimpleWitness w2 = new SimpleWitness("w2", "a e c d");
        SimpleWitness w3 = new SimpleWitness("w3", "a d b");
        SkipgramBasedAligner aligner = new SkipgramBasedAligner();
        aligner.align(w1.getTokens(), w2.getTokens(), w3.getTokens());
    }
}