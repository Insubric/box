package ch.wsl.box.testmodel.boxentities

import ch.wsl.box.rest.runtime._

class GenRegistry() extends RegistryInstance {

    override val routes = GeneratedRoutes
    override val fileRoutes = FileRoutes
    override val actions = EntityActionsRegistry
    override val fields = FieldAccessRegistry
    override val schema = "test_box"
    override val postgisSchema = "test_public"

}
           
