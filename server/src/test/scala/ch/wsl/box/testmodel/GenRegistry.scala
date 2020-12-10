package ch.wsl.box.testmodel

import ch.wsl.box.rest.runtime._

class GenRegistry() extends RegistryInstance {

    override val routes = GeneratedRoutes
    override val fileRoutes = FileRoutes
    override val actions = EntityActionsRegistry
    override val fields: FieldRegistry = TestFieldRegistry
    override def tables: TableRegistry = ???
}
           
