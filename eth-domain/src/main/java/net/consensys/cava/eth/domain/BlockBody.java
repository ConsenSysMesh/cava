package net.consensys.cava.eth.domain;

import static com.google.common.base.Preconditions.checkNotNull;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.rlp.RLP;
import net.consensys.cava.rlp.RLPReader;
import net.consensys.cava.rlp.RLPWriter;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects;

/**
 * An Ethereum block body.
 */
public final class BlockBody {

  /**
   * Deserialize a block body from RLP encoded bytes.
   *
   * @param encoded The RLP encoded block.
   * @return The deserialized block body.
   */
  public static BlockBody fromBytes(Bytes encoded) {
    return RLP.decodeList(encoded, BlockBody::readFrom);
  }

  static BlockBody readFrom(RLPReader reader) {
    List<Transaction> txs = new ArrayList<>();
    reader.readList((listReader, l) -> {
      while (!listReader.isComplete()) {
        txs.add(listReader.readList(Transaction::readFrom));
      }
    });
    List<BlockHeader> ommers = new ArrayList<>();
    reader.readList((listReader, l) -> {
      while (!listReader.isComplete()) {
        ommers.add(listReader.readList(BlockHeader::readFrom));
      }
    });

    return new BlockBody(txs, ommers);
  }

  private final List<Transaction> transactions;
  private final List<BlockHeader> ommers;

  /**
   * Creates a new block body.
   *
   * @param transactions the list of transactions in this block.
   * @param ommers the list of ommers for this block.
   */
  public BlockBody(List<Transaction> transactions, List<BlockHeader> ommers) {
    checkNotNull(transactions);
    checkNotNull(ommers);
    this.transactions = transactions;
    this.ommers = ommers;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BlockBody)) {
      return false;
    }
    BlockBody blockBody = (BlockBody) o;
    return Objects.equal(transactions, blockBody.transactions) && Objects.equal(ommers, blockBody.ommers);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(transactions, ommers);
  }

  /**
   * @return the list of ommers for this block.
   */
  public List<BlockHeader> ommers() {
    return ommers;
  }

  /**
   * @return The RLP serialized form of this block body.
   */
  public Bytes toBytes() {
    return RLP.encodeList(this::writeTo);
  }

  @Override
  public String toString() {
    return "BlockBody{" + "transactions=" + transactions + ", ommers=" + ommers + '}';
  }

  /**
   * @return the transactions of the block.
   */
  public List<Transaction> transactions() {
    return transactions;
  }

  void writeTo(RLPWriter writer) {
    writer.writeList(listWriter -> {
      for (Transaction tx : transactions) {
        listWriter.writeList(tx::writeTo);
      }
    });
    writer.writeList(listWriter -> {
      for (BlockHeader ommer : ommers) {
        listWriter.writeList(ommer::writeTo);
      }
    });
  }
}
