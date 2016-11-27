//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.11 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.07.08 at 06:48:40 PM CEST 
//


package com.logitags.cibet.bindings;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ControlsBinding complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ControlsBinding"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;choice maxOccurs="unbounded"&gt;
 *         &lt;element name="tenant" type="{http://www.w3.org/2001/XMLSchema}token" minOccurs="0"/&gt;
 *         &lt;element name="event" type="{http://www.w3.org/2001/XMLSchema}token" minOccurs="0"/&gt;
 *         &lt;element name="target" type="{http://www.w3.org/2001/XMLSchema}token" minOccurs="0"/&gt;
 *         &lt;element name="method" type="{http://www.w3.org/2001/XMLSchema}token" minOccurs="0"/&gt;
 *         &lt;element name="invoker" type="{http://www.logitags.com}InExAttributeBinding" minOccurs="0"/&gt;
 *         &lt;element name="stateChange" type="{http://www.logitags.com}InExAttributeBinding" minOccurs="0"/&gt;
 *         &lt;element name="condition" type="{http://www.w3.org/2001/XMLSchema}token" minOccurs="0"/&gt;
 *         &lt;element name="customControl" type="{http://www.logitags.com}CustomControlBinding" maxOccurs="unbounded" minOccurs="0"/&gt;
 *       &lt;/choice&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ControlsBinding", propOrder = {
    "tenantOrEventOrTarget"
})
public class ControlsBinding {

    @XmlElementRefs({
        @XmlElementRef(name = "event", namespace = "http://www.logitags.com", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "tenant", namespace = "http://www.logitags.com", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "target", namespace = "http://www.logitags.com", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "condition", namespace = "http://www.logitags.com", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "customControl", namespace = "http://www.logitags.com", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "method", namespace = "http://www.logitags.com", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "invoker", namespace = "http://www.logitags.com", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "stateChange", namespace = "http://www.logitags.com", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<?>> tenantOrEventOrTarget;

    /**
     * Gets the value of the tenantOrEventOrTarget property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the tenantOrEventOrTarget property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTenantOrEventOrTarget().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link CustomControlBinding }{@code >}
     * {@link JAXBElement }{@code <}{@link String }{@code >}
     * {@link JAXBElement }{@code <}{@link InExAttributeBinding }{@code >}
     * {@link JAXBElement }{@code <}{@link InExAttributeBinding }{@code >}
     * 
     * 
     */
    public List<JAXBElement<?>> getTenantOrEventOrTarget() {
        if (tenantOrEventOrTarget == null) {
            tenantOrEventOrTarget = new ArrayList<JAXBElement<?>>();
        }
        return this.tenantOrEventOrTarget;
    }

}
