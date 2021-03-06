/**
 * Copyright (c) 2010, Sebastian Sdorra
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 3. Neither the name of SCM-Manager; nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * http://bitbucket.org/sdorra/scm-manager
 *
 */



package sonia.scm.user.orientdb;

//~--- non-JDK imports --------------------------------------------------------

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OClass.INDEX_TYPE;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

import sonia.scm.orientdb.AbstractConverter;
import sonia.scm.orientdb.Converter;
import sonia.scm.user.User;

//~--- JDK imports ------------------------------------------------------------

import java.util.Map;

/**
 *
 * @author Sebastian Sdorra
 */
public class UserConverter extends AbstractConverter implements Converter<User>
{

  /** Field description */
  public static final String DOCUMENT_CLASS = "User";

  /** Field description */
  public static final String FIELD_ACTIVE = "active";

  /** Field description */
  public static final String FIELD_ADMIN = "admin";

  /** Field description */
  public static final String FIELD_CREATIONDATE = "creationDate";

  /** Field description */
  public static final String FIELD_DISPLAYNAME = "displayName";

  /** Field description */
  public static final String FIELD_MAIL = "mail";

  /** Field description */
  public static final String FIELD_PASSWORD = "password";

  /** Field description */
  public static final String INDEX_ID = "UserId";

  /** Field description */
  public static final UserConverter INSTANCE = new UserConverter();

  //~--- methods --------------------------------------------------------------

  /**
   * Method description
   *
   *
   * @param item
   *
   * @return
   */
  @Override
  public ODocument convert(User item)
  {
    ODocument doc = new ODocument(DOCUMENT_CLASS);

    return convert(doc, item);
  }

  /**
   * Method description
   *
   *
   * @param doc
   * @param user
   *
   * @return
   */
  @Override
  public ODocument convert(ODocument doc, User user)
  {
    appendModelObjectFields(doc, user);
    appendField(doc, FIELD_DISPLAYNAME, user.getDisplayName());
    appendField(doc, FIELD_MAIL, user.getMail());
    appendField(doc, FIELD_PASSWORD, user.getPassword());
    appendField(doc, FIELD_ADMIN, user.isAdmin());
    appendField(doc, FIELD_ACTIVE, user.isActive());
    appendField(doc, FIELD_CREATIONDATE, user.getCreationDate(), OType.LONG);
    appendPropertiesField(doc, user);

    return doc;
  }

  /**
   * Method description
   *
   *
   * @param doc
   *
   * @return
   */
  @Override
  public User convert(ODocument doc)
  {
    User user = new User();

    user.setName(getStringField(doc, FIELD_ID));
    user.setDisplayName(getStringField(doc, FIELD_DISPLAYNAME));
    user.setMail(getStringField(doc, FIELD_MAIL));
    user.setPassword(getStringField(doc, FIELD_PASSWORD));
    user.setType(getStringField(doc, FIELD_TYPE));
    user.setAdmin(getBooleanField(doc, FIELD_ADMIN));
    user.setAdmin(getBooleanField(doc, FIELD_ACTIVE));
    user.setLastModified(getLongField(doc, FIELD_LASTMODIFIED));
    user.setCreationDate(getLongField(doc, FIELD_CREATIONDATE));

    Map<String, String> properties = doc.field(FIELD_PROPERTIES);

    user.setProperties(properties);

    return user;
  }

  /**
   * Method description
   *
   *
   * @param connection
   */
  @Override
  public void createShema(ODatabaseDocumentTx connection)
  {
    OSchema schema = connection.getMetadata().getSchema();
    OClass oclass = schema.getClass(DOCUMENT_CLASS);

    if (oclass == null)
    {
      oclass = schema.createClass(DOCUMENT_CLASS);

      // model properites
      oclass.createProperty(FIELD_ID, OType.STRING);
      oclass.createProperty(FIELD_TYPE, OType.STRING);
      oclass.createProperty(FIELD_LASTMODIFIED, OType.LONG);

      // user properties
      oclass.createProperty(FIELD_ADMIN, OType.BOOLEAN);
      oclass.createProperty(FIELD_CREATIONDATE, OType.LONG);
      oclass.createProperty(FIELD_DISPLAYNAME, OType.STRING);
      oclass.createProperty(FIELD_MAIL, OType.STRING);
      oclass.createProperty(FIELD_ACTIVE, OType.STRING);
      oclass.createProperty(FIELD_PASSWORD, OType.STRING);
      oclass.createProperty(FIELD_PROPERTIES, OType.EMBEDDEDMAP);

      // indexes
      oclass.createIndex(INDEX_ID, INDEX_TYPE.UNIQUE, FIELD_ID);
      schema.save();
    }
  }
}
