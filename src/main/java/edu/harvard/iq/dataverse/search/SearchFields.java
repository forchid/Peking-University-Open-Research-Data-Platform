package edu.harvard.iq.dataverse.search;

/**
 * We define Solr search fields here in one central place so they can be used
 * throughout the code but renamed here if need be.
 *
 * Note that there are many fields in Solr that are *not* here because their
 * values come from the database. For example "authorName" comes from the
 * database. We update the Solr schema.xml file by merging the output of `curl
 * http://localhost:8080/api/admin/index/solr/schema` into the file in the
 * source tree when a metadata block update warrants it.
 *
 * Generally speaking, we want the search fields to be readable. This is a
 * challenge for long field names but a power user should be able to type
 * "authorAffiliation:Harvard" into the general search box. A regular user is
 * much more likely to used Advanced Search to populate that field
 * automatically.
 *
 * Originally, these fields were all snake_case but since the dynamic fields are
 * camelCase we might want to standardize on that.
 *
 * You'll notice that dynamic fields like this are used...
 *
 * - _s (string)
 *
 * - _ss (multivalued string)
 *
 * - _l (long)
 *
 * - _dt (datetime)
 *
 * ... and these endings should not be changed unless you plan to convert them
 * to non-dynamic (by removing the ending) and specify their "type" in the Solr
 * schema.xml.
 *
 * Most fields we want to be searchable but some are stored with indexed=false
 * because we *don't* want them to be searchable and we're just using Solr as a
 * convenient key/value store. Why go to the database if you don't have to? For
 * a string here or there that needs to be available to both the GUI and the
 * Search API, we can just store them in Solr.
 *
 * For faceting we use a "string" type. If you use something like "text_general"
 * the field is tokenized ("Foo Bar" becomes "foo" "bar" which is not what we
 * want). See also
 * http://stackoverflow.com/questions/16559911/facet-query-will-give-wrong-output-on-dynamicfield-in-solr
 */
public class SearchFields {

    /**
     * @todo: consider making various dynamic fields (_s) static in schema.xml
     * instead. Should they be stored in the database?
     */
    // standard fields from example/solr/collection1/conf/schema.xml
    // (but we are getting away from these...)
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String NAME_ZH = "name_zh";
    /**
     * @todo Do we want to support finding dataverses, datasets, and files with
     * a query for description:foo? Maybe not, since people will probably just
     * use basic search for this. They could also use "dvDescription:foo OR
     * dsDescription:foo OR fileDescription:foo" if they *really* only want to
     * target the description of all three types at once.
     *
     * See also https://redmine.hmdc.harvard.edu/issues/3745
     */
    public static final String DESCRIPTION = "description";
    public static final String DESCRIPTION_ZH = "description_zh";
    /**
     * Identifiers differ per DvObject: alias for dataverses, globalId for
     * datasets, and database id for files.
     */
    public static final String IDENTIFIER = "identifier";
    /**
     * Such as http://dx.doi.org/10.5072/FK2/HXI35W
     *
     * For files, the URL will be the parent dataset.
     */
    public static final String PERSISTENT_URL = "persistentUrl";
    public static final String UNF = "unf";
    public static final String DATAVERSE_NAME = "dvName";
    public static final String DATAVERSE_AFFILIATION = "dvAffiliation";
    public static final String DATAVERSE_AFFILIATION_ZH = "dvAffiliation_zh";
    public static final String DATAVERSE_DESCRIPTION = "dvDescription";
    public static final String DATAVERSE_DESCRIPTION_ZH = "dvDescription_zh";
    public static final String DATAVERSE_CATEGORY = "dvCategory";
    /**
     * What is dvSubject_en for? How does it get populated into Solr? The
     * behavior changed so that now the subjects of dataverses are based on
     * their datasets. Should this be a string so we can facet on it more
     * properly? Should all checkboxes on the advanced search page (controlled
     * vocabularies) be backed by a string? When we rename this to "foobar" (a
     * field Solr doesn't know about) why doesn't Solr complain when we "index
     * all"? See also https://github.com/IQSS/dataverse/issues/1681
     */
    public static final String DATAVERSE_SUBJECT = "dvSubject";
    /**
     * A "collapsed" facet (e.g. applies to both dataverses and datasets and is
     * merged as a single facet in the GUI) like affiliation that needs to match
     * the corresponding dynamic "facet" Solr field at the dataset level to work
     * properly. Should we use/expose "_ss" when you click a facet? It needs to
     * be different from "subject" which is used for general search but maybe we
     * could have a convention like "subjectFacet" for the facets?
     */
    public static final String SUBJECT = "subject_ss";
    /**
     * @todo think about how to tie the fact that this needs to be multivalued
     * (_ss) because a multivalued facet (authorAffilition_ss) will be collapsed
     * into it at index time. The business logic to determine if a data-driven
     * metadata field should be indexed into Solr as a single or multiple value
     * lives in the getSolrField() method of DatasetField.java
     *
     * AFFILIATION is used for the "collapsed" "Affiliation" facet that means
     * either "Author Affiliation" or dataverse affiliation. It needs to be a
     * string so we can facet on it and it needs to be multivalued because
     * "Author Affiliation" can be multivalued.
     */
    public static final String AFFILIATION = "affiliation_ss";
    public static final String AFFILIATION_ZH = "affiliation_zh_ss";
    public static final String FILE_NAME = "fileName";
    public static final String FILE_DESCRIPTION = "fileDescription";
    /**
     * Can be multivalued and includes both "friendly" and "group" versions:
     * "PNG Image", "image"
     */
    public static final String FILE_TYPE_SEARCHABLE = "fileType";
    /**
     * @todo Thie static variable not named properly. We want to expose an
     * acutal MIME Type in https://github.com/IQSS/dataverse/issues/1595 . See
     * also cleanup ticket at https://github.com/IQSS/dataverse/issues/1314
     *
     * i.e. "PNG Image"
     */
    public static final String FILE_TYPE_FRIENDLY = "fileTypeDisplay";
    public static final String FILE_CONTENT_TYPE = "fileContentType";
    /**
     * Used as a facet for file groups like "image" or "document"
     */
    public static final String FILE_TYPE = "fileTypeGroupFacet";
    public static final String FILE_SIZE_IN_BYTES = "fileSizeInBytes";
    public static final String FILE_MD5 = "fileMd5";
    public static final String FILENAME_WITHOUT_EXTENSION = "fileNameWithoutExtension";

    public static final String SUBTREE = "subtreePaths";

    // i.e. http://localhost:8080/search.xhtml?q=*&fq0=citationdate_dt:[2008-01-01T00%3A00%3A00Z+TO+2011-01-01T00%3A00%3A00Z%2B1YEAR}
//    public static final String PRODUCTION_DATE_ORIGINAL = DatasetFieldConstant.productionDate + "_dt";
//    public static final String PRODUCTION_DATE_YEAR_ONLY = DatasetFieldConstant.productionDate + "_i";
//    public static final String DISTRIBUTION_DATE_ORIGINAL = DatasetFieldConstant.distributionDate + "_dt";
//    public static final String DISTRIBUTION_DATE_YEAR_ONLY = DatasetFieldConstant.distributionDate + "_i";
    /**
     * Solr refers to "relevance" as "score"
     */
    public static final String RELEVANCE = "score";

    /**
     * A dataverse, a dataset, or a file.
     */
    public static final String TYPE = "dvObjectType";
    public static final String NAME_SORT = "nameSort";
    public static final String PUBLICATION_DATE = "publicationDate";
    public static final String RELEASE_OR_CREATE_DATE = "dateSort";
    /**
     * i.e. "Mar 17, 2015"
     */
    public static final String RELEASE_OR_CREATE_DATE_SEARCHABLE_TEXT = "dateFriendly";
    public static final String RELEASE_OR_CREATE_DATE_SEARCHABLE_TEXT_ZH = "dateFriendly_zh";

    public static final String DEFINITION_POINT = "definitionPointDocId";
    public static final String DEFINITION_POINT_DVOBJECT_ID = "definitionPointDvObjectId";
    public static final String DISCOVERABLE_BY = "discoverableBy";

    /**
     * i.e. "Unpublished", "Draft" (multivalued)
     */
    public static final String PUBLICATION_STATUS = "publicationStatus";
    /**
     * @todo reconcile different with Solr schema.xml where type is Long rather
     * than String.
     */
    public static final String ENTITY_ID = "entityId";
    public static final String PARENT_NAME = "parentName";
    public static final String PARENT_NAME_ZH = "parentName_zh";
    public static final String PARENT_ID = "parentId";
    public static final String PARENT_IDENTIFIER = "parentIdentifier";
    public static final String PARENT_CITATION = "parentCitation";

    public static final String DATASET_DESCRIPTION = "dsDescriptionValue";
    public static final String DATASET_DESCRIPTION_ZH = "dsDescriptionValue_zh";
    public static final String DATASET_CITATION = "citation";
    public static final String DATASET_CITATION_ZH = "citation_zh";
    public static final String DATASET_DEACCESSION_REASON = "deaccessionReason";
    /**
     * In contrast to PUBLICATION_DATE, this field applies only to datasets for
     * more targeted results for just datasets. The format is YYYY (i.e.
     * "2015").
     */
    public static final String DATASET_PUBLICATION_DATE = "dsPublicationDate";
    public static final String DATASET_PERSISTENT_ID = "dsPersistentId";
    public static final String DATASET_VERSION_ID = "datasetVersionId";

    public static final String VARIABLE_NAME = "variableName";
    public static final String VARIABLE_LABEL = "variableLabel";

}
