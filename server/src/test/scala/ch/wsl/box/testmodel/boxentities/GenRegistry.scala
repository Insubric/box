package ch.wsl.box.testmodel.boxentities

import ch.wsl.box.rest.runtime._

class GenRegistry() extends RegistryInstance {

    override val routes = GeneratedRoutes
    override val fileRoutes = FileRoutes
    override val actions = EntityActionsRegistry
    override val fields = FieldAccessRegistry

}
           