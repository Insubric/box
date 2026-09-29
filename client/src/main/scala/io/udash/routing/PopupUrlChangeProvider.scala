package io.udash.routing

import com.avsystem.commons.{MLinkedHashSet, Opt}
import io.udash.core.Url
import io.udash.properties.MutableSetRegistration
import io.udash.utils.Registration

class PopupUrlChangeProvider(base:String) extends UrlChangeProvider {

    var _current:Url = Url(base)
    private val callbacks: MLinkedHashSet[Url => Unit] = MLinkedHashSet.empty

    override def initialize(): Unit = ()

    override def changeFragment(url: Url, replaceCurrent: Boolean): Unit = _current = url

    override def currentFragment: Url = _current

    override def onFragmentChange(callback: Url => Unit): Registration = {
      new MutableSetRegistration( callbacks, callback, Opt.Empty)
    }

}
