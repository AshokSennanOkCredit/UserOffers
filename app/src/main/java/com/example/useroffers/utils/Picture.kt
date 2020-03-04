package com.example.useroffers.utils

import java.io.Serializable

class Picture(val path: String, var selected: Boolean = false) : Serializable
  {
      override fun equals(other: Any?): Boolean {
          if (this === other) return true
          if (other !is Picture) return false

          if (path != other.path) return false

          return true
      }

      override fun hashCode(): Int {
          return path.hashCode()
      }
  }