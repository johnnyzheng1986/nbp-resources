/* Generated By:JJTree: Do not edit this line. ASTTestDefinition.java */

package org.netbeans.performance.benchmarks.bde.generated;

public class ASTTestDefinition extends SimpleNode {
  public ASTTestDefinition(int id) {
    super(id);
  }

  public ASTTestDefinition(BDEParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(BDEParserVisitor visitor, Object data) throws Exception {
    return visitor.visit(this, data);
  }
}
