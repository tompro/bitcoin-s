package org.scalacoin.script.stack

import org.scalacoin.script.ScriptProgramImpl
import org.scalacoin.script.bitwise.OP_EQUAL
import org.scalacoin.script.constant.{OP_1, OP_0, ScriptConstantImpl}
import org.scalacoin.util.{TestUtil, ScalacoinUtil}
import org.scalatest.{FlatSpec, MustMatchers}

/**
 * Created by chris on 1/6/16.
 */
class StackInterpreterTest extends FlatSpec with MustMatchers with StackInterpreter {
  val stack = List(ScriptConstantImpl("Hello"),ScriptConstantImpl("World"))
  "StackInterpreter" must "duplicate elements on top of the stack" in {

    val script = List(OP_DUP)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())
    val newProgram = opDup(program)

    newProgram.stack.head must be (ScriptConstantImpl("Hello"))
    newProgram.stack(1) must be (ScriptConstantImpl("Hello"))
    newProgram.stack(2) must be (ScriptConstantImpl("World"))
  }

  it must "throw an exception when calling opDup without an OP_DUP on top of the script stack" in {

    intercept[IllegalArgumentException] {
      val script = List()
      val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())
      opDup(program)
    }
  }

  it must "throw an exception when calling opDup without an element on top of the stack" in {

    intercept[IllegalArgumentException] {
      val stack = List()
      val script = List(OP_DUP)
      val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())
      opDup(program)
    }
  }

  it must "evaluate the OP_DEPTH operator correctly" in {
    val stack = List(ScriptConstantImpl("Hello"),ScriptConstantImpl("World"))
    val script = List(OP_DEPTH)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())
    val newProgram = opDepth(program)

    newProgram.stack.head.hex must be (ScalacoinUtil.encodeHex(stack.size.toByte))
  }

  it must "evaluate OP_DEPTH operator correctly when there are zero items on the stack" in {
    val stack = List()
    val script = List(OP_DEPTH)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())
    val newProgram = opDepth(program)
    newProgram.stack.head.hex must be (stack.size.toHexString+"0")
  }

  it must "evaluate an OP_TOALTSTACK operator correctly" in {
    val stack = List(OP_0)
    val script = List(OP_TOALTSTACK)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())
    val newProgram = opToAltStack(program)

    newProgram.stack.isEmpty must be (true)
    newProgram.script.isEmpty must be (true)
    newProgram.altStack must be (List(OP_0))

  }

  it must "evaluate an OP_DROP operator correctly" in {
    val stack = List(OP_0)
    val script = List(OP_DROP)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())
    val newProgram = opDrop(program)

    newProgram.stack.isEmpty must be (true)
    newProgram.script.isEmpty must be (true)
  }

  it must "evaluate an OP_IFDUP correctly" in {
    val stack = List(OP_0)
    val script = List(OP_IFDUP)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction, List())
    val newProgram = opIfDup(program)

    newProgram.stack must be (stack)
    newProgram.script.isEmpty must be (true)

    val stack1 = List(OP_1)
    val program1 = ScriptProgramImpl(stack1,script,TestUtil.transaction,List())
    val newProgram1 = opIfDup(program1)
    newProgram1.stack must be (List(OP_1,OP_1))
    newProgram1.script.isEmpty must be (true)

  }

  it must "evaluate an OP_NIP correctly" in {
    val stack = List(OP_0,OP_1)
    val script = List(OP_NIP)

    val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())

    val newProgram = opNip(program)

    newProgram.stack must be (List(OP_0))
    newProgram.script.isEmpty must be (true)
  }

  it must "evaluate an OP_OVER correctly" in {
    val stack = List(OP_0,OP_1)
    val script = List(OP_OVER)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())
    val newProgram = opOver(program)
    newProgram.stack must be (List(OP_1,OP_0,OP_1))
    newProgram.script.isEmpty must be (true)
  }

  it must "evaluate an OP_PICK correctly" in {
    val stack = List(OP_0, ScriptConstantImpl("14"), ScriptConstantImpl("15"), ScriptConstantImpl("16"))
    val script = List(OP_PICK)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())
    val newProgram = opPick(program)

    newProgram.stack must be (List(ScriptConstantImpl("14"),ScriptConstantImpl("14"),
      ScriptConstantImpl("15"), ScriptConstantImpl("16")))
    newProgram.script.isEmpty must be (true)
  }

  it must "evaluate an OP_ROLL correctly" in {
    val stack = List(OP_0, ScriptConstantImpl("14"), ScriptConstantImpl("15"), ScriptConstantImpl("16"))
    val script = List(OP_ROLL)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())
    val newProgram = opRoll(program)

    newProgram.stack must be (List(ScriptConstantImpl("14"),
      ScriptConstantImpl("15"), ScriptConstantImpl("16")))
    newProgram.script.isEmpty must be (true)

  }

  it must "evaluate an OP_ROT correctly" in {
    val stack = List(ScriptConstantImpl("14"), ScriptConstantImpl("15"), ScriptConstantImpl("16"))
    val script = List(OP_ROT)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())
    val newProgram = opRot(program)

    newProgram.stack must be (List(ScriptConstantImpl("16"),ScriptConstantImpl("14"),ScriptConstantImpl("15")))
    newProgram.script.isEmpty must be (true)
  }

  it must "evaluate an OP_2ROT correctly" in {
    val stack = List(ScriptConstantImpl("14"), ScriptConstantImpl("15"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19"))
    val script = List(OP_2ROT)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction,List())
    val newProgram = op2Rot(program)

    newProgram.stack must be (List(ScriptConstantImpl("18"),ScriptConstantImpl("19"),ScriptConstantImpl("14"),
      ScriptConstantImpl("15"),ScriptConstantImpl("16"), ScriptConstantImpl("17")))
    newProgram.script.isEmpty must be (true)
  }

  it must "evalauate an OP_2DROP correctly" in {
    val stack = List(ScriptConstantImpl("14"), ScriptConstantImpl("15"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19"))
    val script = List(OP_2DROP)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction, List())
    val newProgram = op2Drop(program)

    newProgram.stack must be (List(ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19")))
  }


  it must "evaluate an OP_SWAP correctly" in {
    val stack = List(ScriptConstantImpl("14"), ScriptConstantImpl("15"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19"))
    val script = List(OP_SWAP)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction, List())
    val newProgram = opSwap(program)
    newProgram.stack must be (List(ScriptConstantImpl("15"),ScriptConstantImpl("14"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19")))
    newProgram.script.isEmpty must be (true)
  }

  it must "evaluate an OP_TUCK correctly" in  {
    val stack = List(ScriptConstantImpl("14"), ScriptConstantImpl("15"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19"))
    val script = List(OP_TUCK)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction, List())
    val newProgram = opTuck(program)
    newProgram.stack must be (List(ScriptConstantImpl("15"),ScriptConstantImpl("14"),ScriptConstantImpl("15"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19")))
    newProgram.script.isEmpty must be (true)
  }

  it must "evaluate an OP_2DUP correctly" in  {
    val stack = List(ScriptConstantImpl("14"), ScriptConstantImpl("15"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19"))
    val script = List(OP_2DUP)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction, List())
    val newProgram = op2Dup(program)
    newProgram.stack must be (List(ScriptConstantImpl("14"),ScriptConstantImpl("15"),
      ScriptConstantImpl("14"),ScriptConstantImpl("15"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19")))
    newProgram.script.isEmpty must be (true)
  }

  it must "evaluate an OP_3DUP correctly" in {
    val stack = List(ScriptConstantImpl("14"), ScriptConstantImpl("15"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19"))
    val script = List(OP_3DUP)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction, List())
    val newProgram = op3Dup(program)

    newProgram.stack must be (List(ScriptConstantImpl("14"),ScriptConstantImpl("15"),ScriptConstantImpl("16"),
      ScriptConstantImpl("14"),ScriptConstantImpl("15"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19")))
    newProgram.script.isEmpty must be (true)
  }

  it must "evaluate an OP_2OVER correctly" in {
    val stack = List(ScriptConstantImpl("14"), ScriptConstantImpl("15"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19"))
    val script = List(OP_2OVER)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction, List())
    val newProgram = op2Over(program)

    newProgram.stack must be (List(ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("14"),ScriptConstantImpl("15"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19")))
    newProgram.script.isEmpty must be (true)
  }


  it must "evaluate an OP_2SWAP correctly" in {
    val stack = List(ScriptConstantImpl("14"), ScriptConstantImpl("15"), ScriptConstantImpl("16"),
      ScriptConstantImpl("17"), ScriptConstantImpl("18"), ScriptConstantImpl("19"))
    val script = List(OP_2SWAP)
    val program = ScriptProgramImpl(stack,script,TestUtil.transaction, List())
    val newProgram = op2Swap(program)

    newProgram.stack must be (List(ScriptConstantImpl("16"), ScriptConstantImpl("17"),ScriptConstantImpl("14"),
      ScriptConstantImpl("15"), ScriptConstantImpl("18"), ScriptConstantImpl("19")))
    newProgram.script.isEmpty must be (true)
  }
}
