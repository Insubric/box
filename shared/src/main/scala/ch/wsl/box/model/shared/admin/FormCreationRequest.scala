package ch.wsl.box.model.shared.admin

case class ChildForm(entity:String, childs:Seq[ChildForm])

case class FormCreationRequest(
                              name:String,
                              add_to_home:Boolean,
                              roles:Seq[String],
                              main_entity:String,
                              childs: Seq[ChildForm]
                              )
object FormCreationRequest {
  def base(entity:String) = FormCreationRequest("",true,Seq(),entity,Seq())
}