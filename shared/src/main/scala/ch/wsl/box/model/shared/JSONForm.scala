package ch.wsl.box.model.shared

/**
  * Created by andre on 5/16/2017.
  */
case class JSONForm(id:Int,name:String,fields:Seq[JSONField],layout:Layout, table:String,lang:String, tableFields:Seq[String],keys:Seq[String])