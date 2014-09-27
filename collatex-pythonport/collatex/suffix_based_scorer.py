'''
Created on Aug 5, 2014

@author: Ronald Haentjens Dekker
'''
from sets import Set
from collatex.collatex_suffix import Occurrence, BlockWitness, Block,\
    PartialOverlapException
from operator import attrgetter
from ClusterShell.RangeSet import RangeSet

'''
 This class scores cells in the edit graph table based on the largest non overlapping blocks
 that are found in the witness set.
'''

class Scorer(object):
    def __init__(self, collation):
        self.collation = collation
        self.blocks = []
        self.global_tokens_to_occurrences = {}
        
    def prepare_witness(self, witness):
        # this code can probably done more efficient, for now the main goal is to make it work
        block_witness = self._get_block_witness(witness)
        tokens_to_occurrences = self._build_tokens_to_occurrences(self.collation, witness, block_witness)
        #NOTE: we do not have to store all tokens from the witness
        #NOTE: if we split the dict into two: one for next witness and one for superbase
        #NOTE: only the ones that are going to be aligned have to be stored in superbase dict.
        self.global_tokens_to_occurrences.update(tokens_to_occurrences)
        
    #TODO: it should be possible to do this simpler, faster
    # An occurrence should know its tokens, since it knows its token range
    def _build_tokens_to_occurrences(self, collation, witness, block_witness):
        tokens_to_occurrence = {}
        witness_range = collation.get_range_for_witness(witness.sigil)
        token_counter = witness_range[0]
        # note: this can be done faster by focusing on the occurrences
        # instead of the tokens
        for token in witness.tokens():
            for occurrence in block_witness.occurrences:
                if occurrence.is_in_range(token_counter):
                    tokens_to_occurrence[token]=occurrence
            token_counter += 1
        return tokens_to_occurrence

    
    def score_cell(self, token_a, token_b, y, x, table):
        # initialize root node score to zero (no edit operations have
        # been performed)
        if y == 0 and x == 0:
            return 0 
        # examine neighbor nodes
        nodes_to_examine = Set()
        # fetch existing score from the left node if possible
        if x > 0:
            nodes_to_examine.add(table[y][x-1])
        if y > 0:
            nodes_to_examine.add(table[y-1][x])
        if x > 0 and y > 0:
            nodes_to_examine.add(table[y-1][x-1])
        # calculate the maximum scoring parent node
        parent_node = max(nodes_to_examine, key=lambda x: x.g)
        # no matching possible in this case (always treated as a gap)
        # it is either an add or a delete
        if x == 0 or y == 0:
            return parent_node.g - 1
         
        # it is either an add/delete or replacement (so an add and a delete)
        # it is a replacement
        if parent_node == table[y-1][x-1]:
            # now we need to determine whether this node represents a match
            # determine this based on whether token a and token b are part of the same block
            occur_a = self.global_tokens_to_occurrences.setdefault(token_a, None)
            occur_b = self.global_tokens_to_occurrences.setdefault(token_b, None)
                                                                
            if occur_a and occur_b:
                match = occur_a.block == occur_b.block
            else:
                match = False
#             print("testing "+token_a.token_string+" and "+token_b.token_string+" "+str(match))   
            # match = token_a.token_string == token_b.token_string
            # based on match or not and parent_node calculate new score
            if match:
                # mark the fact that this node is match
                table[y][x].match = True
                # do not change score for now 
                score = parent_node.g
                # count segments
                if parent_node.match == False:
                    table[y][x].segments = parent_node.segments + 1
             
            else:
                score = parent_node.g - 2
        # it is an add/delete
        else:
            score = parent_node.g - 1
        return score


    '''
    Internal method to transform a Witness into a Block Witness.
    '''
    def _get_block_witness(self, witness):
#         print("Generating block witness for: "+witness.sigil)
        sigil_witness = witness.sigil
        range_witness = self.collation.get_range_for_witness(sigil_witness)
        #NOTE: to prevent recalculation of blocks
        if not self.blocks:
            self.blocks = self._get_non_overlapping_repeating_blocks() 
        blocks = self.blocks 
        # make a selection of blocks and occurrences of these blocks in the selected witness
        occurrences = []
        for block in blocks:
            block_ranges_in_witness = block.ranges & range_witness
            # note this are multiple ranges
            # we need to iterate over every single one
            for block_range in block_ranges_in_witness.contiguous():
                occurrence = Occurrence(block_range, block)
                occurrences.append(occurrence) 
        # sort occurrences on position
        sorted_o = sorted(occurrences, key=attrgetter('lower_end'))
        block_witness = BlockWitness(sorted_o, self.collation.tokens)
        return block_witness

    '''
    Find all the non overlapping repeating blocks in the witness set of this collation.
    '''
    def _get_non_overlapping_repeating_blocks(self):
        extended_suffix_array = self.collation.to_extended_suffix_array()
        potential_blocks = extended_suffix_array.split_lcp_array_into_intervals() 
        self.filter_potential_blocks(potential_blocks)
        # step 3: sort the blocks based on depth (number of repetitions) first,
        # second length of LCP interval,
        # third sort on parent LCP interval occurrences.
        sorted_blocks_on_priority = sorted(potential_blocks, key=attrgetter("number_of_occurrences", "minimum_block_length", "number_of_siblings"), reverse=True)
        # step 4: select the definitive blocks
        occupied = RangeSet()
        real_blocks = []
        for potential_block in sorted_blocks_on_priority:
#           print(potential_block.info())
            try:
                non_overlapping_range = potential_block.calculate_non_overlapping_range_with(occupied)
                if non_overlapping_range:
#                     print("Selecting: "+str(potential_block))
                    occupied.union_update(non_overlapping_range)
                    real_blocks.append(Block(non_overlapping_range))
            except PartialOverlapException:          
#                 print("Skip due to conflict: "+str(potential_block))
                while potential_block.minimum_block_length > 1:
                    # retry with a different length: one less
                    for idx in range(potential_block.start+1, potential_block.end+1):
                        potential_block.LCP[idx] -= 1
                    potential_block.length -= 1
                    try:
                        non_overlapping_range = potential_block.calculate_non_overlapping_range_with(occupied)
                        if non_overlapping_range:
#                             print("Retried and selecting: "+str(potential_block))
                            occupied.union_update(non_overlapping_range)
                            real_blocks.append(Block(non_overlapping_range))
                            break
                    except PartialOverlapException:          
#                         print("Retried and failed again")
                        pass
        return real_blocks


    # filter out all the blocks that have more than one occurrence within a witness
    def filter_potential_blocks(self, potential_blocks):
        for potential_block in potential_blocks[:]:
            for witness in self.collation.witnesses:
                witness_sigil = witness.sigil
                witness_range = self.collation.get_range_for_witness(witness_sigil)
                inter = witness_range.intersection(potential_block.block_occurrences())
                if potential_block.number_of_occurrences > len(self.collation.witnesses) or len(inter)> potential_block.minimum_block_length:
#                     print("Removing block: "+str(potential_block))
                    potential_blocks.remove(potential_block)
                    break
