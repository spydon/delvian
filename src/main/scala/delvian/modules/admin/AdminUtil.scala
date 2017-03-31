package delvian.modules.admin

import java.io._

import akka.util.Timeout

import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

object AdminUtil {
  private val filename = "users.txt"
  private val lowAccessScript = List("psu")

  def addUser(user: String, admin: String): String = {
    if(hasAccess(admin)) {
      val file = new File(filename)
      val bw = new BufferedWriter(new FileWriter(file, true))
      bw.write(s"$user\n")
      bw.close()
      s"$user added"
    } else {
      s"$admin is not an admin"
    }
  }

  def deleteUser(user: String, admin: String): String = {

    if(hasAccess(admin)) {
      val admins = Source.fromFile(filename).getLines()
      val file = new File(filename)
      val bw = new BufferedWriter(new FileWriter(file, false))
      bw.write(admins.filterNot(_ == user).mkString("\\n"))
      bw.close()
      s"$user deleted"
    } else {
      s"You (<@$admin>) are not an admin"
    }
  }

  def hasAccess(user: String): Boolean = Source.fromFile(filename).getLines().contains(s"<@$user>")
  def lowAccess(script: String): Boolean = lowAccessScript.contains(script)

  def listAdmins(): String = {
    try {
      val admins = Source.fromFile(filename).getLines().mkString("\\n")

      if(admins != "") admins
      else "No admins found :neutral_face:"
    } catch {
      case ex: FileNotFoundException => "Couldn't find that file."
      case ex: IOException => "Had an IOException trying to read the file"
    }
  }

}
