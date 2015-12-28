package org.hotsextra.matchmaking.entries

sealed trait Role
case object Warrior extends Role
case object Assassin extends Role
case object Support extends Role
case object Specialist extends Role