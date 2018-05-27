package net.consensys.cava.eth.domain;

import static java.util.Objects.requireNonNull;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.rlp.RLP;
import net.consensys.cava.rlp.RLPReader;
import net.consensys.cava.rlp.RLPWriter;

import com.google.common.base.Objects;

/**
 * An Ethereum block.
 */
public final class Block {

  /**
   * Deserialize a block from RLP encoded bytes.
   *
   * @param encoded The RLP encoded block.
   * @return The deserialized block.
   */
  public static Block fromBytes(Bytes encoded) {
    return RLP.decodeList(encoded, Block::readFrom);
  }

  /**
   * Deserialize a block from an RLP input.
   *
   * @param reader The RLP reader.
   * @return The deserialized block.
   */
  static Block readFrom(RLPReader reader) {
    BlockHeader header = reader.readList(BlockHeader::readFrom);
    BlockBody body = BlockBody.readFrom(reader);
    return new Block(header, body);
  }

  private final BlockHeader header;
  private final BlockBody body;

  /**
   * Creates a block.
   *
   * @param header the header of the block.
   * @param body the body of the block.
   */
  public Block(BlockHeader header, BlockBody body) {
    requireNonNull(header);
    requireNonNull(body);
    this.header = header;
    this.body = body;
  }

  /**
   * @return the block body.
   */
  public BlockBody body() {
    return body;
  }

  /**
   * @return the block header.
   */
  public BlockHeader header() {
    return header;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Block)) {
      return false;
    }
    Block other = (Block) obj;
    return Objects.equal(header, other.header) && Objects.equal(body, other.body);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(header, body);
  }

  @Override
  public String toString() {
    return "Block{" + "header=" + header + ", body=" + body + '}';
  }

  /**
   * @return The RLP serialized form of this block.
   */
  public Bytes toBytes() {
    return RLP.encodeList(this::writeTo);
  }

  /**
   * Write this block to an RLP output.
   *
   * @param writer The RLP writer.
   */
  private void writeTo(RLPWriter writer) {
    writer.writeList(header::writeTo);
    body.writeTo(writer);
  }
}
