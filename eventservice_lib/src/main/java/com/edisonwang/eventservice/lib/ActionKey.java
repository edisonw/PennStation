package com.edisonwang.eventservice.lib;

import java.io.Serializable;

/**
 * @author edi
 */
public interface ActionKey extends Serializable {
    BaseAction value();
}
