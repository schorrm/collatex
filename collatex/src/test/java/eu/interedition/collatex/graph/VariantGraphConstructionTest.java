package eu.interedition.collatex.graph;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import eu.interedition.collatex.AbstractTest;
import eu.interedition.collatex.CollationAlgorithmFactory;
import eu.interedition.collatex.Token;
import eu.interedition.collatex.Witness;
import eu.interedition.collatex.input.SimpleToken;
import eu.interedition.collatex.input.SimpleWitness;
import eu.interedition.collatex.matching.EqualityTokenComparator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;
import java.util.SortedMap;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class VariantGraphConstructionTest extends AbstractTest {
  
  @Before
  public void switchCollationAlgorithm() {
    collationAlgorithm = CollationAlgorithmFactory.needlemanWunsch(new EqualityTokenComparator());
  }

  @Test
  public void collateDarwinSample() {
    final VariantGraph graph = collate(//
            "It has been disputed at what period of life the causes of variability, whatever they may be, generally act; whether during the early or late period of development of the embryo, or at the instant of conception. Geoffroy St. Hilaire's experiments show that unnatural treatment of the embryo causes monstrosities; and monstrosities cannot be separated by any clear line of distinction from mere variations. But I am strongly inclined to suspect that the most frequent cause of variability may be attributed to the male and female reproductive elements having been affected prior to the act of conception. Several reasons make me believe in this; but the chief one is the remarkable effect which confinement or cultivation has on the functions of the reproductive system; this system appearing to be far more susceptible than any other part of the organisation, to the action of any change in the conditions of life. Nothing is more easy than to tame an animal, and few things more difficult than to get it to breed freely under confinement, even in the many cases when the male and female unite. How many animals there are which will not breed, though living long under not very close confinement in their native country! This is generally attributed to vitiated instincts; but how many cultivated plants display the utmost vigour, and yet rarely or never seed! In some few such cases it has been found out that very trifling changes, such as a little more or less water at some particular period of growth, will determine whether or not the plant sets a seed. I cannot here enter on the copious details which I have collected on this curious subject; but to show how singular the laws are which determine the reproduction of animals under confinement, I may just mention that carnivorous animals, even from the tropics, breed in this country pretty freely under confinement, with the exception of the plantigrades or bear family; whereas, carnivorous birds, with the rarest exceptions, hardly ever lay fertile eggs. Many exotic plants have pollen utterly worthless, in the same exact condition as in the most sterile hybrids. When, on the one hand, we see domesticated animals and plants, though often weak and sickly, yet breeding quite freely under confinement; and when, on the other hand, we see individuals, though taken young from a state of nature, perfectly tamed, long-lived, and healthy (of which I could give numerous instances), yet having their reproductive system so seriously affected by unperceived causes as to fail in acting, we need not be surprised at this system, when it does act under confinement, acting not quite regularly, and producing offspring not perfectly like their parents or variable.",//
            "With respect to what I have called the indirect action of changed conditions, namely, through the reproductive system being affected, we may infer that variability is thus induced, partly from the fact of this system being extremely sensitive to any change in the conditions, and partly from the similarity, as Kölreuter and others have remarked, between the variability which follows from the crossing of distinct species, and that which may be observed with all plants and animals when reared under new or unnatural conditions. Many facts clearly show how eminently susceptible the reproductive system is to very slight changes in the surrounding conditions. Nothing is more easy than to tame an animal, and few things more difficult than to get it to breed freely under confinement, even when the male and female unite. How many animals there are which will not breed, though kept in an almost free state in their native country! This is generally, but erroneously, attributed to vitiated instincts. Many cultivated plants display the utmost vigour, and yet rarely or never seed! In some few cases it has been discovered that a very trifling change, such as a little more or less water at some particular period of growth, will determine whether or not a plant will produce seeds. I cannot here give the details which I have collected and elsewhere published on this curious subject; but to show how singular the laws are which determine the reproduction of animals under confinement, I may mention that carnivorous animals, even from the tropics, breed in this country pretty freely under confinement, with the exception of the plantigrades or bear family, which seldom produce young; whereas carnivorous birds, with the rarest exceptions, hardly ever lay fertile eggs. Many exotic plants have pollen utterly worthless, in the same condition as in the most sterile hybrids. When, on the one hand, we see domesticated animals and plants, though often weak and sickly, yet breeding freely under confinement; and when, on the other hand, we see individuals, though taken young from a state of nature, perfectly tamed, long-lived, and healthy (of which I could give numerous instances), yet having their reproductive system so seriously affected by unperceived causes as to fail to act, we need not be surprised at this system, when it does act under confinement, acting irregularly, and producing offspring somewhat unlike their parents. I may add, that as some organisms breed freely under the most unnatural conditions (for instance, rabbits and ferrets kept in hutches), showing that their reproductive organs are not affected; so will some animals and plants withstand domestication or cultivation, and vary very slightly — perhaps hardly more than in a state of nature.");

    //graph.join();
    /*
    for (Set<VariantGraphVertex> rank : graph.rank().ranks()) {
      for (VariantGraphVertex vertex : rank) {
        final SortedSetMultimap<Witness, SimpleToken> tokens = TreeMultimap.create(SimpleWitness.SIGIL_COMPARATOR, Ordering.natural());
        for (SimpleToken token : Iterables.filter(vertex.tokens(), SimpleToken.class)) {
          tokens.put(token.getWitness(), token);
        }
        final StringBuilder segment = new StringBuilder();
        for (Witness witness : tokens.keySet()) {
          segment.append(witness.getSigil()).append(": [").append(SimpleToken.toString(tokens.get(witness))).append("] ");
        }
        System.out.println(segment.toString().trim());
      }
    }
    */
  }
}
